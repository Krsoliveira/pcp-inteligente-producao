import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
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
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettingsOutlined'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined'
import EditIcon from '@mui/icons-material/EditOutlined'
import VisibilityIcon from '@mui/icons-material/VisibilityOutlined'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { excluirPerfil, listarPerfis, listarPermissoes } from '../api/acessos'
import { PerfilDialog } from '../components/acessos/PerfilDialog'
import { CelulaDeCabecalho, ResumoDaTabela, useTabelaInterativa, type Coluna } from '../components/consulta/TabelaInterativa'
import { EmptyState } from '../components/EmptyState'
import { PageHeader } from '../components/PageHeader'
import { SePermitido } from '../components/SePermitido'
import { usePermissoes } from '../hooks/usePermissoes'
import type { PerfilAcesso } from '../types'
import { formatarDataHora } from '../utils/formatacao'
import { mensagemDeErroAoCarregar, mensagemDeErroDaApi } from '../utils/mensagensDeErro'

const COLUNAS: Coluna<PerfilAcesso>[] = [
  { chave: 'nome', rotulo: 'Perfil', valor: (p) => p.nome },
  { chave: 'descricao', rotulo: 'Descrição', valor: (p) => p.descricao },
  { chave: 'permissoes', rotulo: 'Permissões', valor: (p) => p.permissoes.length, texto: (p) => `${p.permissoes.length}` },
  { chave: 'usuarios', rotulo: 'Usuários', valor: (p) => p.quantidadeDeUsuarios },
  { chave: 'atualizado', rotulo: 'Atualizado', valor: (p) => p.atualizadoEm, texto: (p) => `${formatarDataHora(p.atualizadoEm)} ${p.atualizadoPor}` },
]

/**
 * Perfis de acesso (ADR-0020): o gestor cria perfis e marca o que cada um pode fazer. Só se concede o
 * que se tem; o perfil Administrador e o padrão do autocadastro são de sistema.
 */
export function PerfisAcessoPage() {
  const queryClient = useQueryClient()
  const { permissoes, tem } = usePermissoes()
  const [dialogo, setDialogo] = useState<{ aberto: boolean; perfil: PerfilAcesso | null }>({ aberto: false, perfil: null })
  const [excluindo, setExcluindo] = useState<PerfilAcesso | null>(null)
  const [erroExclusao, setErroExclusao] = useState<string | null>(null)

  const perfisQuery = useQuery({ queryKey: ['acessos', 'perfis'], queryFn: listarPerfis })
  const catalogoQuery = useQuery({ queryKey: ['acessos', 'permissoes'], queryFn: listarPermissoes, staleTime: Infinity })
  const perfis = perfisQuery.data ?? []
  const tabela = useTabelaInterativa(perfis, COLUNAS)

  const { mutate: excluir, isPending: excluindoAgora } = useMutation({
    mutationFn: (id: string) => excluirPerfil(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['acessos'] })
      setExcluindo(null)
      setErroExclusao(null)
    },
    onError: (e: unknown) => setErroExclusao(mensagemDeErroDaApi(e, 'Não foi possível excluir o perfil.')),
  })

  const podeGerenciar = tem('PERFIL_GERENCIAR')
  const podeExcluir = (p: PerfilAcesso) =>
    podeGerenciar && !p.administrador && !p.padraoDoAutocadastro && p.quantidadeDeUsuarios === 0 && p.permissoes.every((x) => permissoes.has(x))

  return (
    <Box>
      <PageHeader
        titulo="Perfis de acesso"
        subtitulo="Cada usuário tem um perfil; o perfil define o que ele pode fazer no sistema"
        breadcrumbs={[{ label: 'Administração' }, { label: 'Perfis de acesso' }]}
        action={
          <SePermitido qualquer={['PERFIL_GERENCIAR']}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogo({ aberto: true, perfil: null })}>
              Novo perfil
            </Button>
          </SePermitido>
        }
      />

      {perfisQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(perfisQuery.error, 'Não foi possível carregar os perfis.')}</Alert>
      )}

      <Card>
        {perfisQuery.isLoading ? (
          <Skeleton variant="rounded" height={240} sx={{ m: 2 }} />
        ) : perfis.length === 0 ? (
          <EmptyState Icone={AdminPanelSettingsIcon} titulo="Nenhum perfil cadastrado" />
        ) : (
          <>
            <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={perfis.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    {COLUNAS.map((c) => (
                      <CelulaDeCabecalho key={c.chave} chave={c.chave} rotulo={c.rotulo} controle={tabela.controle}
                        align={c.chave === 'permissoes' || c.chave === 'usuarios' ? 'center' : undefined} />
                    ))}
                    <TableCell align="center" sx={{ width: 110 }}>Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tabela.linhas.map((p) => (
                    <TableRow key={p.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>{p.nome}</Typography>
                          {(p.administrador || p.padraoDoAutocadastro) && <Chip label="Sistema" size="small" variant="outlined" sx={{ fontSize: '0.65rem' }} />}
                        </Box>
                      </TableCell>
                      <TableCell><Typography variant="body2" sx={{ color: 'text.secondary' }}>{p.descricao || '—'}</Typography></TableCell>
                      <TableCell align="center">{p.administrador ? 'Todas' : p.permissoes.length}</TableCell>
                      <TableCell align="center">{p.quantidadeDeUsuarios}</TableCell>
                      <TableCell>
                        <Typography variant="body2" noWrap>{formatarDataHora(p.atualizadoEm)}</Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>{p.atualizadoPor}</Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Tooltip title={podeGerenciar && !p.administrador ? 'Editar' : 'Ver permissões'}>
                          <IconButton size="small" aria-label={`${podeGerenciar && !p.administrador ? 'Editar' : 'Ver'} perfil ${p.nome}`}
                            onClick={() => setDialogo({ aberto: true, perfil: p })}>
                            {podeGerenciar && !p.administrador ? <EditIcon sx={{ fontSize: 18 }} /> : <VisibilityIcon sx={{ fontSize: 18 }} />}
                          </IconButton>
                        </Tooltip>
                        {podeExcluir(p) && (
                          <Tooltip title="Excluir">
                            <IconButton size="small" color="error" aria-label={`Excluir perfil ${p.nome}`}
                              onClick={() => { setErroExclusao(null); setExcluindo(p) }}>
                              <DeleteOutlineIcon sx={{ fontSize: 18 }} />
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

      <PerfilDialog
        aberto={dialogo.aberto}
        onFechar={() => setDialogo((d) => ({ ...d, aberto: false }))}
        perfil={dialogo.perfil}
        catalogo={catalogoQuery.data ?? []}
        permissoesDoUsuario={permissoes}
        podeGerenciar={podeGerenciar}
        podeVerHistorico={tem('AUDITORIA_VISUALIZAR')}
      />

      <Dialog open={excluindo !== null} onClose={excluindoAgora ? undefined : () => setExcluindo(null)} fullWidth maxWidth="xs">
        <DialogTitle>Excluir o perfil {excluindo?.nome}?</DialogTitle>
        <DialogContent>
          {erroExclusao && <Alert severity="error" sx={{ mb: 2 }}>{erroExclusao}</Alert>}
          <DialogContentText>
            O perfil sai da lista e não pode ser recuperado. Nenhum usuário o usa, então ninguém perde acesso.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setExcluindo(null)} disabled={excluindoAgora}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={() => excluindo && excluir(excluindo.id)} disabled={excluindoAgora}>
            {excluindoAgora ? 'Excluindo…' : 'Excluir perfil'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
