import { useState } from 'react'
import { usePermissoes } from '../hooks/usePermissoes'
import { useNavigate, useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid'
import Skeleton from '@mui/material/Skeleton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import BlockIcon from '@mui/icons-material/Block'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import CompareArrowsIcon from '@mui/icons-material/CompareArrows'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ativarListaTecnica, buscarListaTecnicaPorId, listarListasTecnicas } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { InfoRow } from '../components/InfoRow'
import { PageHeader } from '../components/PageHeader'
import { StatusListaBadge } from '../components/StatusBadge'
import { RetirarDeUsoDialog } from '../components/listas/RetirarDeUsoDialog'
import { CriarListaTecnicaDialog } from '../components/listas/CriarListaTecnicaDialog'
import { TIPO_COR } from '../components/materiais/MaterialTable'
import { formatarCodigoMaterial, formatarDataHora, formatarQuantidade } from '../utils/formatacao'
import { mensagemDeErroAoCarregar, mensagemDeErroDaApi } from '../utils/mensagensDeErro'
import { TIPO_MATERIAL_LABEL } from '../types'

/** Detalhe da lista técnica: a "receita" (componentes e quantidades) de uma versão. */
export function ListaTecnicaDetalhePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { tem } = usePermissoes()
  const queryClient = useQueryClient()
  const [retirando, setRetirando] = useState(false)
  const [copiando, setCopiando] = useState(false)
  const listaQuery = useQuery({ queryKey: ['listas-tecnicas', 'detalhe', id], queryFn: () => buscarListaTecnicaPorId(id!), enabled: !!id })
  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))
  const lista = listaQuery.data
  const { data: versoes = [] } = useQuery({
    queryKey: ['listas-tecnicas', 'versoes', lista?.materialId],
    queryFn: () => listarListasTecnicas(lista!.materialId),
    enabled: !!lista,
  })
  const outrasVersoes = versoes.filter((v) => v.id !== id)

  const { mutate: ativar, isPending: ativando, error: erroAtivar } = useMutation({
    mutationFn: () => ativarListaTecnica(id!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] }),
  })

  // Depois de criar, abre a versão nova (o cadastro não devolve o id, então procura pelo rótulo).
  const irParaVersaoCriada = async (materialId: string, rotulo: string) => {
    const doMaterial = await queryClient.fetchQuery({ queryKey: ['listas-tecnicas', 'versoes', materialId], queryFn: () => listarListasTecnicas(materialId), staleTime: 0 })
    const nova = doMaterial.find((v) => v.versao === rotulo)
    if (nova) navigate(`/listas-tecnicas/${nova.id}`)
  }

  if (listaQuery.isLoading) return <Skeleton variant="rounded" height={320} />
  if (listaQuery.isError || !lista) {
    return <Alert severity="error">{mensagemDeErroAoCarregar(listaQuery.error, 'Lista técnica não encontrada.')}</Alert>
  }

  const material = materialPorId.get(lista.materialId)
  const codigo = material ? formatarCodigoMaterial(material.codigo) : '—'

  return (
    <Box>
      <PageHeader
        titulo={material?.descricao ?? 'Lista técnica'}
        subtitulo={`${codigo} · versão ${lista.versao}`}
        breadcrumbs={[
          { label: 'Cadastros' },
          { label: 'Listas Técnicas', href: '/listas-tecnicas' },
          { label: `${codigo} · ${lista.versao}` },
        ]}
        action={
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {tem('LISTA_CADASTRAR') && (
              <Button variant="outlined" startIcon={<ContentCopyIcon />} onClick={() => setCopiando(true)}>
                Nova versão a partir desta
              </Button>
            )}
            {lista.status === 'EM_REVISAO' && tem('LISTA_ATIVAR') ? (
              <Button variant="contained" color="success" startIcon={<CheckCircleOutlineIcon />} onClick={() => ativar()} disabled={ativando}>
                {ativando ? 'Ativando…' : 'Ativar esta versão'}
              </Button>
            ) : lista.status === 'ATIVA' && tem('LISTA_RETIRAR_DE_USO') ? (
              <Button variant="outlined" color="warning" startIcon={<BlockIcon />} onClick={() => setRetirando(true)}>
                Retirar de uso
              </Button>
            ) : null}
          </Box>
        }
      />

      {erroAtivar && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroDaApi(erroAtivar, 'Não foi possível ativar a lista técnica.')}</Alert>}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <AccountTreeIcon color="action" fontSize="small" />
                <Typography variant="subtitle2">Lista técnica</Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={1.5}>
                <InfoRow label="Material">{material ? `${codigo} — ${material.descricao}` : '—'}</InfoRow>
                <InfoRow label="Versão">
                  <Chip label={lista.versao} size="small" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }} />
                </InfoRow>
                <InfoRow label="Quantidade base">
                  {`${formatarQuantidade(lista.quantidadeBase, material?.unidadeDeMedida)}`}
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                    Os componentes valem para produzir essa quantidade.
                  </Typography>
                </InfoRow>
                <InfoRow label="Status"><StatusListaBadge status={lista.status} /></InfoRow>
                <InfoRow label="Componentes">{String(lista.itens.length)}</InfoRow>
                <InfoRow label="Criada">
                  <Typography variant="body2">{formatarDataHora(lista.criadaEm)}</Typography>
                  <Typography variant="caption" color="text.secondary">{lista.criadaPor}</Typography>
                </InfoRow>
                <InfoRow label="Alterada">
                  <Typography variant="body2">{formatarDataHora(lista.atualizadaEm)}</Typography>
                  <Typography variant="caption" color="text.secondary">{lista.atualizadaPor}</Typography>
                </InfoRow>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, lg: 8 }}>
          <Card sx={{ height: '100%' }}>
            <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Typography variant="subtitle2">Componentes</Typography>
            </Box>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ width: 40 }}>#</TableCell>
                    <TableCell>Código</TableCell>
                    <TableCell>Descrição</TableCell>
                    <TableCell>Tipo</TableCell>
                    <TableCell align="right">Quantidade</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {lista.itens.map((item, i) => {
                    const componente = materialPorId.get(item.materialComponenteId)
                    return (
                      <TableRow key={item.id} hover>
                        <TableCell><Typography variant="body2" color="text.secondary">{i + 1}</Typography></TableCell>
                        <TableCell>
                          <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }} noWrap>
                            {componente ? formatarCodigoMaterial(componente.codigo) : '—'}
                          </Typography>
                        </TableCell>
                        <TableCell>{componente?.descricao ?? `${item.materialComponenteId.substring(0, 8)}…`}</TableCell>
                        <TableCell>
                          {componente && (
                            <Chip label={TIPO_MATERIAL_LABEL[componente.tipo]} size="small"
                              sx={{ bgcolor: `${TIPO_COR[componente.tipo]}14`, color: TIPO_COR[componente.tipo], fontWeight: 600, fontSize: '0.7rem', border: 'none' }} />
                          )}
                        </TableCell>
                        <TableCell align="right">{formatarQuantidade(item.quantidadePlanejada, item.unidadeDeMedida)}</TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
        </Grid>

        {outrasVersoes.length > 0 && (
          <Grid size={12}>
            <Card>
              <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
                <Typography variant="subtitle2">Outras versões desta lista</Typography>
              </Box>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Versão</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell align="center">Componentes</TableCell>
                      <TableCell align="center" sx={{ width: 140 }}>Ações</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {outrasVersoes.map((v) => (
                      <TableRow key={v.id} hover>
                        <TableCell>
                          <Button size="small" onClick={() => navigate(`/listas-tecnicas/${v.id}`)} sx={{ fontFamily: 'monospace' }}>{v.versao}</Button>
                        </TableCell>
                        <TableCell><StatusListaBadge status={v.status} /></TableCell>
                        <TableCell align="center">{v.itens.length}</TableCell>
                        <TableCell align="center">
                          {tem('LISTA_COMPARAR') && (
                          <Button
                            size="small"
                            startIcon={<CompareArrowsIcon />}
                            aria-label={`Comparar com a versão ${v.versao}`}
                            onClick={() => navigate(`/listas-tecnicas/comparar?a=${lista.id}&b=${v.id}`)}
                          >
                            Comparar
                          </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Card>
          </Grid>
        )}
      </Grid>

      <CriarListaTecnicaDialog
        aberto={copiando}
        onFechar={() => setCopiando(false)}
        modelo={lista}
        versoesExistentes={versoes.map((v) => v.versao)}
        onCriada={irParaVersaoCriada}
      />

      <RetirarDeUsoDialog
        lista={retirando ? lista : null}
        nomeDoMaterial={material ? `${codigo} — ${material.descricao}` : 'Material'}
        onFechar={() => setRetirando(false)}
      />
    </Box>
  )
}
