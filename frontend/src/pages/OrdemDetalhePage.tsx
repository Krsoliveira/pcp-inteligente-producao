import { useState } from 'react'
import { useParams } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Grid from '@mui/material/Grid2'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import TextField from '@mui/material/TextField'
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import EditIcon from '@mui/icons-material/Edit'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import DoneAllIcon from '@mui/icons-material/DoneAll'
import AllInboxIcon from '@mui/icons-material/AllInboxOutlined'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { buscarOrdemPorId, concluirOrdem } from '../api/ordens'
import { listarConsumosPorOrdem, registrarConsumo } from '../api/consumos'
import { listarLotesPorOrdem } from '../api/lotes'
import { buscarListaTecnicaPorId } from '../api/listasTecnicas'
import { buscarMaterialPorId, listarMateriais } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { StatusOrdemBadge, StatusLoteBadge } from '../components/StatusBadge'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import type { ConsumoMaterial, ConcluirOrdemRequest, RegistrarConsumoRequest } from '../types'

export function OrdemDetalhePage() {
  const { id } = useParams<{ id: string }>()
  const queryClient = useQueryClient()
  const [consumoSelecionado, setConsumoSelecionado] = useState<ConsumoMaterial | null>(null)
  const [dialogConcluir, setDialogConcluir] = useState(false)
  const [dialogStatus, setDialogStatus] = useState(false)

  const ordemQuery = useQuery({
    queryKey: ['ordem', id],
    queryFn: () => buscarOrdemPorId(id!),
    enabled: !!id,
  })

  const consumosQuery = useQuery({
    queryKey: ['consumos', id],
    queryFn: () => listarConsumosPorOrdem(id!),
    enabled: !!id,
  })

  const lotesQuery = useQuery({
    queryKey: ['lotes-ordem', id],
    queryFn: () => listarLotesPorOrdem(id!),
    enabled: !!id,
  })

  const listaTecnicaQuery = useQuery({
    queryKey: ['lista-tecnica', ordemQuery.data?.listaTecnicaId],
    queryFn: () => buscarListaTecnicaPorId(ordemQuery.data!.listaTecnicaId),
    enabled: !!ordemQuery.data?.listaTecnicaId,
  })

  // Catálogo de materiais (compartilhado em cache) para exibir código em vez de ID.
  const materiaisQuery = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map((materiaisQuery.data ?? []).map((m) => [m.id, m]))
  const rotuloMaterial = (id: string) => {
    const m = materialPorId.get(id)
    return (
      <Tooltip title={m?.descricao ?? ''}>
        <Typography variant="body2" fontWeight={500}>{m?.codigo ?? '…'}</Typography>
      </Tooltip>
    )
  }

  const materialQuery = useQuery({
    queryKey: ['material', ordemQuery.data?.materialId],
    queryFn: () => buscarMaterialPorId(ordemQuery.data!.materialId),
    enabled: !!ordemQuery.data?.materialId,
  })

  const invalidar = () => {
    queryClient.invalidateQueries({ queryKey: ['ordem', id] })
    queryClient.invalidateQueries({ queryKey: ['consumos', id] })
    queryClient.invalidateQueries({ queryKey: ['lotes-ordem', id] })
    queryClient.invalidateQueries({ queryKey: ['ordens'] })
  }

  const ordem = ordemQuery.data
  const consumos = consumosQuery.data ?? []
  const lotes = lotesQuery.data ?? []
  const listaTecnica = listaTecnicaQuery.data
  const material = materialQuery.data

  const todosRegistrados = consumos.length > 0 && consumos.every((c) => c.registrado)
  const todosJustificados = consumos.every((c) => c.justificado)
  const podeConcluir = ordem?.status === 'EM_PRODUCAO' && todosRegistrados && todosJustificados

  if (ordemQuery.isLoading) {
    return (
      <Box>
        <Skeleton variant="rounded" height={60} sx={{ mb: 2 }} />
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}><Skeleton variant="rounded" height={180} /></Grid>
          <Grid size={{ xs: 12, md: 6 }}><Skeleton variant="rounded" height={180} /></Grid>
        </Grid>
      </Box>
    )
  }

  if (ordemQuery.isError || !ordem) {
    return <Alert severity="error">Ordem não encontrada.</Alert>
  }

  return (
    <Box>
      <PageHeader
        titulo={ordem.codigo}
        subtitulo="Detalhes da ordem de produção"
        breadcrumbs={[
          { label: 'Produção' },
          { label: 'Ordens', href: '/ordens' },
          { label: ordem.codigo },
        ]}
        action={
          <Box sx={{ display: 'flex', gap: 1 }}>
            {!['CONCLUIDA', 'CANCELADA'].includes(ordem.status) && (
              <Button variant="outlined" onClick={() => setDialogStatus(true)}>
                Alterar Status
              </Button>
            )}
            {podeConcluir && (
              <Button variant="contained" color="success" startIcon={<DoneAllIcon />} onClick={() => setDialogConcluir(true)}>
                Concluir Ordem
              </Button>
            )}
          </Box>
        }
      />

      <Grid container spacing={2}>
        {/* Card: Informações */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="subtitle2" gutterBottom>Informações da Ordem</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={1.5}>
                <InfoRow label="Status"><StatusOrdemBadge status={ordem.status} size="medium" /></InfoRow>
                <InfoRow label="Material">{material ? `${material.codigo} — ${material.descricao}` : <Skeleton width={140} />}</InfoRow>
                <InfoRow label="Lista Técnica"><Chip label={listaTecnica?.versao ?? '…'} size="small" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }} /></InfoRow>
                <InfoRow label="Centro de Trabalho">{ordem.centroDeTrabalho}</InfoRow>
                <InfoRow label="Quantidade planejada">{ordem.quantidade.toLocaleString('pt-BR')} {material?.unidadeDeMedida}</InfoRow>
                {ordem.quantidadeProduzida != null && (
                  <InfoRow label="Quantidade produzida">
                    <Typography variant="body2" fontWeight={600} color="success.main">
                      {Number(ordem.quantidadeProduzida).toLocaleString('pt-BR')} {material?.unidadeDeMedida}
                    </Typography>
                  </InfoRow>
                )}
                <InfoRow label="Início planejado">{formatarData(ordem.inicioPlanejado)}</InfoRow>
                <InfoRow label="Fim planejado">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                    <Typography variant="body2">{formatarData(ordem.fimPlanejado)}</Typography>
                    {ordem.atrasada && <Chip label="Atrasada" size="small" color="error" sx={{ height: 18, fontSize: '0.65rem' }} />}
                  </Box>
                </InfoRow>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Card: BOM */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="subtitle2" gutterBottom>Lista Técnica utilizada</Typography>
              <Divider sx={{ mb: 2 }} />
              {!listaTecnica ? (
                <Skeleton variant="rounded" height={100} />
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Componente</TableCell>
                        <TableCell align="right">Qtd / un</TableCell>
                        <TableCell>Un</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {listaTecnica.itens.map((item) => (
                        <TableRow key={item.id}>
                          <TableCell>
                            {rotuloMaterial(item.materialComponenteId)}
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="body2">{Number(item.quantidadePlanejada).toLocaleString('pt-BR')}</Typography>
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" color="text.secondary">{item.unidadeDeMedida}</Typography>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Card: Consumo de Materiais */}
        <Grid size={12}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="subtitle2">Consumo de Materiais</Typography>
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                  {consumos.length > 0 && (
                    <Typography variant="caption" color="text.secondary">
                      {consumos.filter(c => c.registrado).length}/{consumos.length} registrados
                    </Typography>
                  )}
                  {!todosRegistrados && ordem.status === 'EM_PRODUCAO' && (
                    <Chip label="Pendente" size="small" color="warning" variant="outlined" sx={{ fontSize: '0.7rem', height: 20 }} />
                  )}
                  {todosRegistrados && todosJustificados && (
                    <Chip icon={<CheckCircleOutlineIcon sx={{ fontSize: '14px !important' }} />} label="Tudo registrado" size="small" color="success" variant="outlined" sx={{ fontSize: '0.7rem', height: 20 }} />
                  )}
                </Box>
              </Box>
              <Divider sx={{ mb: 2 }} />
              {consumosQuery.isLoading ? (
                <Skeleton variant="rounded" height={120} />
              ) : consumos.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                  Nenhum consumo projetado. Verifique a lista técnica.
                </Typography>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Material</TableCell>
                        <TableCell align="right">Planejado</TableCell>
                        <TableCell align="right">Consumido</TableCell>
                        <TableCell align="right">Desvio</TableCell>
                        <TableCell>Un</TableCell>
                        <TableCell>Situação</TableCell>
                        {ordem.status === 'EM_PRODUCAO' && <TableCell align="center" sx={{ width: 60 }}>Ação</TableCell>}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {consumos.map((c) => {
                        const desvio = c.desvio ?? 0
                        const temDesvio = c.registrado && Math.abs(desvio) > 0.0001
                        return (
                          <TableRow key={c.id}>
                            <TableCell>
                              {rotuloMaterial(c.materialId)}
                            </TableCell>
                            <TableCell align="right"><Typography variant="body2">{c.quantidadePlanejada.toLocaleString('pt-BR')}</Typography></TableCell>
                            <TableCell align="right">
                              {c.registrado ? (
                                <Typography variant="body2">{c.quantidadeConsumida?.toLocaleString('pt-BR')}</Typography>
                              ) : (
                                <Typography variant="body2" color="text.disabled">—</Typography>
                              )}
                            </TableCell>
                            <TableCell align="right">
                              {c.registrado && (
                                <Typography variant="body2" color={temDesvio ? (desvio > 0 ? 'error.main' : 'warning.main') : 'text.secondary'} fontWeight={temDesvio ? 600 : 400}>
                                  {desvio > 0 ? '+' : ''}{desvio.toLocaleString('pt-BR')}
                                </Typography>
                              )}
                            </TableCell>
                            <TableCell><Typography variant="body2" color="text.secondary">{c.unidadeDeMedida}</Typography></TableCell>
                            <TableCell>
                              {!c.registrado ? (
                                <Chip label="Pendente" size="small" sx={{ bgcolor: '#fff3e0', color: '#e65100', fontSize: '0.65rem', height: 18, border: 'none' }} />
                              ) : !c.justificado ? (
                                <Tooltip title="Desvio sem justificativa"><Chip icon={<WarningAmberIcon sx={{ fontSize: '12px !important' }} />} label="Sem justif." size="small" color="error" sx={{ fontSize: '0.65rem', height: 18 }} /></Tooltip>
                              ) : (
                                <Chip icon={<CheckCircleOutlineIcon sx={{ fontSize: '12px !important' }} />} label="OK" size="small" color="success" sx={{ fontSize: '0.65rem', height: 18 }} />
                              )}
                            </TableCell>
                            {ordem.status === 'EM_PRODUCAO' && (
                              <TableCell align="center">
                                {!c.registrado && (
                                  <Tooltip title="Registrar consumo">
                                    <IconButton size="small" color="primary" onClick={() => setConsumoSelecionado(c)}>
                                      <EditIcon sx={{ fontSize: 16 }} />
                                    </IconButton>
                                  </Tooltip>
                                )}
                              </TableCell>
                            )}
                          </TableRow>
                        )
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Card: Lote gerado */}
        {lotes.length > 0 && (
          <Grid size={12}>
            <Card>
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <AllInboxIcon color="action" fontSize="small" />
                  <Typography variant="subtitle2">Lote Gerado</Typography>
                </Box>
                <Divider sx={{ mb: 2 }} />
                {lotes.map((lote) => (
                  <Grid key={lote.id} container spacing={2}>
                    <InfoRow label="Número do Lote">
                      <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }}>{lote.numeroLote}</Typography>
                    </InfoRow>
                    <InfoRow label="Quantidade">{Number(lote.quantidade).toLocaleString('pt-BR')} {lote.unidadeDeMedida}</InfoRow>
                    <InfoRow label="Fabricação">{formatarData(lote.dataFabricacao)}</InfoRow>
                    <InfoRow label="Validade">{formatarData(lote.dataValidade)}</InfoRow>
                    <InfoRow label="Status"><StatusLoteBadge status={lote.status} /></InfoRow>
                  </Grid>
                ))}
              </CardContent>
            </Card>
          </Grid>
        )}
      </Grid>

      {/* Dialog: Registrar consumo */}
      <RegistrarConsumoDialog
        consumo={consumoSelecionado}
        ordemId={id!}
        onFechar={() => setConsumoSelecionado(null)}
        onSucesso={invalidar}
      />

      {/* Dialog: Concluir ordem */}
      <ConcluirOrdemDialog
        aberto={dialogConcluir}
        ordemId={id!}
        onFechar={() => setDialogConcluir(false)}
        onSucesso={() => { invalidar(); setDialogConcluir(false) }}
      />

      {/* Dialog: Alterar status */}
      <AtualizarStatusDialog
        ordem={dialogStatus ? ordem : null}
        onFechar={() => { setDialogStatus(false); invalidar() }}
      />
    </Box>
  )
}

// ---- Helper: linha de informação ----
function InfoRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <>
      <Grid size={{ xs: 12, sm: 5 }}>
        <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ textTransform: 'uppercase', letterSpacing: '0.04em' }}>
          {label}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, sm: 7 }}>
        {typeof children === 'string' ? (
          <Typography variant="body2">{children}</Typography>
        ) : children}
      </Grid>
    </>
  )
}

// ---- Dialog: Registrar consumo ----

interface RegistrarConsumoDialogProps {
  consumo: ConsumoMaterial | null
  ordemId: string
  onFechar: () => void
  onSucesso: () => void
}

function RegistrarConsumoDialog({ consumo, ordemId, onFechar, onSucesso }: RegistrarConsumoDialogProps) {
  const [qtd, setQtd] = useState('')
  const [justificativa, setJustificativa] = useState('')
  const [responsavel, setResponsavel] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  const desvio = consumo && qtd ? Number(qtd) - consumo.quantidadePlanejada : 0
  const temDesvio = Math.abs(desvio) > 0.0001

  const { mutate, isPending } = useMutation({
    mutationFn: (payload: RegistrarConsumoRequest) => registrarConsumo(ordemId, consumo!.id, payload),
    onSuccess: () => { onSucesso(); onFechar(); setQtd(''); setJustificativa(''); setResponsavel(''); setErro(null) },
    onError: (err: unknown) => { if (axios.isAxiosError(err)) setErro(err.response?.data?.detail ?? 'Erro.'); else setErro('Erro inesperado.') },
  })

  const handleSubmit = () => {
    if (!qtd) { setErro('Informe a quantidade consumida.'); return }
    const payload: RegistrarConsumoRequest = { quantidadeConsumida: Number(qtd), justificativa: justificativa || undefined, justificadoPor: responsavel || undefined }
    mutate(payload)
  }

  return (
    <Dialog open={!!consumo} onClose={onFechar} fullWidth maxWidth="xs">
      <DialogTitle>Registrar Consumo</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        {consumo && (
          <Box sx={{ mb: 2, p: 1.5, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Typography variant="caption" color="text.secondary">Quantidade planejada</Typography>
            <Typography variant="body2" fontWeight={600}>{consumo.quantidadePlanejada.toLocaleString('pt-BR')} {consumo.unidadeDeMedida}</Typography>
          </Box>
        )}
        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField label="Quantidade consumida *" type="number" value={qtd} onChange={(e) => setQtd(e.target.value)} fullWidth inputProps={{ min: 0, step: 0.0001 }}
              helperText={qtd && temDesvio ? `Desvio: ${desvio > 0 ? '+' : ''}${desvio.toLocaleString('pt-BR')} ${consumo?.unidadeDeMedida}` : ''} />
          </Grid>
          {temDesvio && (
            <>
              <Grid size={12}>
                <Alert severity="warning" sx={{ py: 0.5 }}>Desvio detectado. Justificativa obrigatória.</Alert>
              </Grid>
              <Grid size={12}>
                <TextField label="Justificativa *" value={justificativa} onChange={(e) => setJustificativa(e.target.value)} fullWidth multiline rows={2} />
              </Grid>
              <Grid size={12}>
                <TextField label="Responsável *" value={responsavel} onChange={(e) => setResponsavel(e.target.value)} fullWidth />
              </Grid>
            </>
          )}
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>{isPending ? 'Salvando…' : 'Registrar'}</Button>
      </DialogActions>
    </Dialog>
  )
}

// ---- Dialog: Concluir Ordem ----

interface ConcluirOrdemDialogProps {
  aberto: boolean
  ordemId: string
  onFechar: () => void
  onSucesso: () => void
}

function ConcluirOrdemDialog({ aberto, ordemId, onFechar, onSucesso }: ConcluirOrdemDialogProps) {
  const hoje = new Date().toISOString().split('T')[0]
  const [form, setForm] = useState<ConcluirOrdemRequest>({ quantidadeProduzida: 0, dataFabricacao: hoje, dataValidade: '' })
  const [erro, setErro] = useState<string | null>(null)

  const { mutate, isPending } = useMutation({
    mutationFn: (payload: ConcluirOrdemRequest) => concluirOrdem(ordemId, payload),
    onSuccess: () => { onSucesso(); setErro(null) },
    onError: (err: unknown) => { if (axios.isAxiosError(err)) setErro(err.response?.data?.detail ?? 'Erro.'); else setErro('Erro inesperado.') },
  })

  const handleSubmit = () => {
    if (!form.quantidadeProduzida || !form.dataFabricacao || !form.dataValidade) { setErro('Preencha todos os campos.'); return }
    mutate(form)
  }

  return (
    <Dialog open={aberto} onClose={onFechar} fullWidth maxWidth="xs">
      <DialogTitle>Concluir Ordem de Produção</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <Alert severity="info" sx={{ mb: 2, py: 0.5 }}>
          Um lote rastreável será gerado automaticamente com o número sequencial.
        </Alert>
        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField label="Quantidade produzida *" type="number" value={form.quantidadeProduzida || ''} onChange={(e) => setForm(p => ({ ...p, quantidadeProduzida: Number(e.target.value) }))} fullWidth inputProps={{ min: 0.0001, step: 0.0001 }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Data de fabricação *" type="date" value={form.dataFabricacao} onChange={(e) => setForm(p => ({ ...p, dataFabricacao: e.target.value }))} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Data de validade *" type="date" value={form.dataValidade} onChange={(e) => setForm(p => ({ ...p, dataValidade: e.target.value }))} fullWidth slotProps={{ inputLabel: { shrink: true } }} helperText="Defina um prazo mesmo sem vencimento natural." />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" color="success" onClick={handleSubmit} disabled={isPending}>{isPending ? 'Concluindo…' : 'Confirmar Conclusão'}</Button>
      </DialogActions>
    </Dialog>
  )
}

function formatarData(data: string): string {
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}
