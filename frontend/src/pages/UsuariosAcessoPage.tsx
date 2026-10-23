import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import IconButton from '@mui/material/IconButton'
import Skeleton from '@mui/material/Skeleton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import GroupsIcon from '@mui/icons-material/GroupsOutlined'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import { useQuery } from '@tanstack/react-query'
import { listarPerfis, listarUsuarios } from '../api/acessos'
import { NovoUsuarioDialog, TrocarPerfilDialog } from '../components/acessos/UsuarioDialogs'
import { CelulaDeCabecalho, ResumoDaTabela, useTabelaInterativa, type Coluna } from '../components/consulta/TabelaInterativa'
import { EmptyState } from '../components/EmptyState'
import { PageHeader } from '../components/PageHeader'
import { SePermitido } from '../components/SePermitido'
import { usePermissoes } from '../hooks/usePermissoes'
import type { UsuarioAcesso } from '../types'
import { formatarDataHora } from '../utils/formatacao'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'

const COLUNAS: Coluna<UsuarioAcesso>[] = [
  { chave: 'nome', rotulo: 'Nome', valor: (u) => u.nome },
  { chave: 'email', rotulo: 'E-mail', valor: (u) => u.email },
  { chave: 'perfil', rotulo: 'Perfil', valor: (u) => u.perfilNome },
  { chave: 'criado', rotulo: 'Criado em', valor: (u) => u.criadoEm, texto: (u) => formatarDataHora(u.criadoEm) },
]

/** Usuários e o perfil de cada um (ADR-0020): o gestor cria usuários e troca perfis, tudo rastreado. */
export function UsuariosAcessoPage() {
  const { usuario: eu, permissoes, tem } = usePermissoes()
  const [novoAberto, setNovoAberto] = useState(false)
  const [trocando, setTrocando] = useState<UsuarioAcesso | null>(null)

  const usuariosQuery = useQuery({ queryKey: ['acessos', 'usuarios'], queryFn: listarUsuarios })
  const perfisQuery = useQuery({ queryKey: ['acessos', 'perfis'], queryFn: listarPerfis })
  const usuarios = usuariosQuery.data ?? []
  const perfis = perfisQuery.data ?? []
  const tabela = useTabelaInterativa(usuarios, COLUNAS)

  const podeGerenciar = tem('USUARIO_GERENCIAR')
  // Ninguém altera o próprio perfil, nem o de quem tem um perfil acima do seu alcance (o servidor recusa).
  const podeTrocar = (u: UsuarioAcesso) => {
    if (!podeGerenciar || u.email.toLowerCase() === eu?.email.toLowerCase()) return false
    const atual = perfis.find((p) => p.id === u.perfilId)
    return !atual || atual.permissoes.every((x) => permissoes.has(x))
  }

  return (
    <Box>
      <PageHeader
        titulo="Usuários"
        subtitulo="Quem acessa o sistema e com qual perfil"
        breadcrumbs={[{ label: 'Administração' }, { label: 'Usuários' }]}
        action={
          <SePermitido qualquer={['USUARIO_GERENCIAR']}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setNovoAberto(true)}>Novo usuário</Button>
          </SePermitido>
        }
      />

      {usuariosQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(usuariosQuery.error, 'Não foi possível carregar os usuários.')}</Alert>
      )}

      <Card>
        {usuariosQuery.isLoading ? (
          <Skeleton variant="rounded" height={240} sx={{ m: 2 }} />
        ) : usuarios.length === 0 ? (
          <EmptyState Icone={GroupsIcon} titulo="Nenhum usuário cadastrado" />
        ) : (
          <>
            <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={usuarios.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    {COLUNAS.map((c) => <CelulaDeCabecalho key={c.chave} chave={c.chave} rotulo={c.rotulo} controle={tabela.controle} />)}
                    <TableCell align="center" sx={{ width: 90 }}>Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tabela.linhas.map((u) => (
                    <TableRow key={u.id} hover>
                      <TableCell><Typography variant="body2" fontWeight={600}>{u.nome}</Typography></TableCell>
                      <TableCell><Typography variant="body2" color="text.secondary">{u.email}</Typography></TableCell>
                      <TableCell>{u.perfilNome ?? '—'}</TableCell>
                      <TableCell><Typography variant="body2" noWrap>{formatarDataHora(u.criadoEm)}</Typography></TableCell>
                      <TableCell align="center">
                        {podeTrocar(u) && (
                          <Tooltip title="Alterar perfil">
                            <IconButton size="small" aria-label={`Alterar o perfil de ${u.nome}`} onClick={() => setTrocando(u)}>
                              <SwapHorizIcon sx={{ fontSize: 18 }} />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </>
        )}
      </Card>

      <NovoUsuarioDialog aberto={novoAberto} onFechar={() => setNovoAberto(false)} perfis={perfis} permissoesDoUsuario={permissoes} />
      <TrocarPerfilDialog usuario={trocando} onFechar={() => setTrocando(null)} perfis={perfis} permissoesDoUsuario={permissoes} />
    </Box>
  )
}
