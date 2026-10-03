import { useParams, Link as RouterLink } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid2'
import Link from '@mui/material/Link'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import LocalShippingIcon from '@mui/icons-material/LocalShippingOutlined'
import PrecisionManufacturingIcon from '@mui/icons-material/PrecisionManufacturingOutlined'
import InventoryIcon from '@mui/icons-material/Inventory2Outlined'
import ScienceIcon from '@mui/icons-material/ScienceOutlined'
import CallSplitIcon from '@mui/icons-material/CallSplit'
import LinearProgress from '@mui/material/LinearProgress'
import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { buscarLotePorId, rastrearLote } from '../api/lotes'
import { buscarOrdemPorId } from '../api/ordens'
import { listarConsumosPorOrdem } from '../api/consumos'
import { buscarListaTecnicaPorId } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { InfoRow } from '../components/InfoRow'
import { HistoricoAuditoria } from '../components/HistoricoAuditoria'
import { StatusLoteBadge, StatusOrdemBadge } from '../components/StatusBadge'
import { diasAte, formatarData, formatarDataHora, formatarQuantidade } from '../utils/formatacao'
import type { ConsumoMaterial, LoteOrigem, Lote, Material, RastreabilidadeLote } from '../types'

/**
 * Detalhe de um lote: identificação e saldo, origem (nota fiscal ou ordem de produção),
 * o que foi consumido para produzi-lo — com os lotes de onde saiu cada material —,
 * onde ele foi usado e o histórico de auditoria. Navegando de lote em lote chega-se
 * do produto acabado à nota fiscal da matéria-prima (ADR-0011).
 */
export function LoteDetalhePage() {
  const { id } = useParams<{ id: string }>()

  const loteQuery = useQuery({
    queryKey: ['lote', id],
    queryFn: () => buscarLotePorId(id!),
    enabled: !!id,
  })
  const lote = loteQuery.data

  const rastreioQuery = useQuery({
    queryKey: ['lote-rastreabilidade', id],
    queryFn: () => rastrearLote(id!),
    enabled: !!id,
  })

  const materiaisQuery = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map((materiaisQuery.data ?? []).map((m) => [m.id, m]))

  if (loteQuery.isLoading) {
    return (
      <Box>
        <Skeleton variant="rounded" height={60} sx={{ mb: 2 }} />
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}><Skeleton variant="rounded" height={220} /></Grid>
          <Grid size={{ xs: 12, md: 6 }}><Skeleton variant="rounded" height={220} /></Grid>
        </Grid>
      </Box>
    )
  }

  if (loteQuery.isError || !lote) {
    return <Alert severity="error">Lote não encontrado.</Alert>
  }

  const material = materialPorId.get(lote.materialId)

  return (
    <Box>
      <PageHeader
        titulo={lote.numeroLote}
        subtitulo={lote.origem === 'COMPRA' ? 'Lote de compra (entrada de material)' : 'Lote de produção'}
        breadcrumbs={[
          { label: 'Produção' },
          { label: 'Lotes', href: '/lotes' },
          { label: lote.numeroLote },
        ]}
      />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <IdentificacaoCard lote={lote} material={material} />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          {lote.origem === 'COMPRA'
            ? <OrigemCompraCard lote={lote} />
            : <OrigemProducaoCard lote={lote} />}
        </Grid>

        {lote.origem === 'PRODUCAO' && lote.ordemProducaoId && (
          <Grid size={12}>
            <MateriaisConsumidosCard ordemId={lote.ordemProducaoId} materialPorId={materialPorId}
              rastreio={rastreioQuery.data} />
          </Grid>
        )}

        <Grid size={12}>
          <OndeFoiUsadoCard lote={lote} rastreio={rastreioQuery.data} carregando={rastreioQuery.isLoading}
            erro={rastreioQuery.isError} materialPorId={materialPorId} />
        </Grid>

        <Grid size={12}>
          <HistoricoAuditoria tipoEntidade="LOTE" entidadeId={lote.id} titulo="Histórico do lote" />
        </Grid>
      </Grid>
    </Box>
  )
}

// ---- Cards ----

function CardSecao({ titulo, icone, children }: { titulo: string; icone: ReactNode; children: ReactNode }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          {icone}
          <Typography variant="subtitle2">{titulo}</Typography>
        </Box>
        <Divider sx={{ mb: 2 }} />
        {children}
      </CardContent>
    </Card>
  )
}

function IdentificacaoCard({ lote, material }: { lote: Lote; material?: Material }) {
  const dias = diasAte(lote.dataValidade)
  const alertaValidade = lote.status === 'DISPONIVEL' && dias <= 30

  return (
    <CardSecao titulo="Identificação" icone={<InventoryIcon color="action" fontSize="small" />}>
      <Grid container spacing={1.5}>
        <InfoRow label="Status"><StatusLoteBadge status={lote.status} /></InfoRow>
        <InfoRow label="Material">{material ? `${material.codigo} — ${material.descricao}` : '…'}</InfoRow>
        <InfoRow label="Quantidade">{formatarQuantidade(lote.quantidade, lote.unidadeDeMedida)}</InfoRow>
        <InfoRow label="Saldo">
          <Box>
            <Typography variant="body2" fontWeight={600}>
              {formatarQuantidade(lote.saldo, lote.unidadeDeMedida)}
              <Typography component="span" variant="caption" color="text.secondary">
                {` · ${Math.round((lote.saldo / lote.quantidade) * 100)}% disponível`}
              </Typography>
            </Typography>
            <LinearProgress variant="determinate" value={Math.min(100, (lote.saldo / lote.quantidade) * 100)}
              aria-label="Saldo do lote" sx={{ mt: 0.5, height: 6, borderRadius: 3, maxWidth: 220 }} />
          </Box>
        </InfoRow>
        <InfoRow label="Fabricação">{formatarData(lote.dataFabricacao)}</InfoRow>
        <InfoRow label="Validade">
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, flexWrap: 'wrap' }}>
            <Typography variant="body2">{formatarData(lote.dataValidade)}</Typography>
            {alertaValidade && (
              <Chip
                size="small"
                color={dias < 0 ? 'error' : 'warning'}
                label={dias < 0 ? `Vencido há ${-dias} dias` : `Vence em ${dias} dias`}
                sx={{ height: 18, fontSize: '0.65rem' }}
              />
            )}
          </Box>
        </InfoRow>
        <InfoRow label="Última alteração">
          <Assinatura usuario={lote.atualizadoPor} instante={lote.atualizadoEm} />
        </InfoRow>
      </Grid>
    </CardSecao>
  )
}

function OrigemCompraCard({ lote }: { lote: Lote }) {
  return (
    <CardSecao titulo="Origem: compra" icone={<LocalShippingIcon color="action" fontSize="small" />}>
      <Grid container spacing={1.5}>
        <InfoRow label="Fornecedor">{lote.fornecedor ?? '—'}</InfoRow>
        <InfoRow label="Nota fiscal">
          <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace', overflowWrap: 'anywhere' }}>
            {lote.notaFiscal ?? '—'}
          </Typography>
        </InfoRow>
        <InfoRow label="Emissão da NF">{formatarData(lote.dataEmissaoNf)}</InfoRow>
        <InfoRow label="Recebimento">{formatarData(lote.dataRecebimento)}</InfoRow>
        <InfoRow label="Entrada registrada">
          <Assinatura usuario={lote.criadoPor} instante={lote.criadoEm} />
        </InfoRow>
      </Grid>
    </CardSecao>
  )
}

function OrigemProducaoCard({ lote }: { lote: Lote }) {
  const ordemQuery = useQuery({
    queryKey: ['ordem', lote.ordemProducaoId],
    queryFn: () => buscarOrdemPorId(lote.ordemProducaoId!),
    enabled: !!lote.ordemProducaoId,
  })
  const ordem = ordemQuery.data

  const listaQuery = useQuery({
    queryKey: ['lista-tecnica', ordem?.listaTecnicaId],
    queryFn: () => buscarListaTecnicaPorId(ordem!.listaTecnicaId),
    enabled: !!ordem?.listaTecnicaId,
  })

  return (
    <CardSecao titulo="Origem: produção" icone={<PrecisionManufacturingIcon color="action" fontSize="small" />}>
      {ordemQuery.isError ? (
        <Alert severity="error">Não foi possível carregar a ordem de origem.</Alert>
      ) : !ordem ? (
        <Skeleton variant="rounded" height={140} />
      ) : (
        <Grid container spacing={1.5}>
          <InfoRow label="Ordem de produção">
            <Link component={RouterLink} to={`/ordens/${ordem.id}`} variant="body2" fontWeight={600}>
              {ordem.codigo}
            </Link>
          </InfoRow>
          <InfoRow label="Status da ordem"><StatusOrdemBadge status={ordem.status} /></InfoRow>
          <InfoRow label="Centro de trabalho">{ordem.centroDeTrabalho}</InfoRow>
          <InfoRow label="Lista técnica">
            <Chip label={listaQuery.data?.versao ?? '…'} size="small" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }} />
          </InfoRow>
          <InfoRow label="Qtd planejada">{formatarQuantidade(ordem.quantidade, lote.unidadeDeMedida)}</InfoRow>
          <InfoRow label="Qtd produzida">{formatarQuantidade(ordem.quantidadeProduzida, lote.unidadeDeMedida)}</InfoRow>
          <InfoRow label="Lote gerado">
            <Assinatura usuario={lote.criadoPor} instante={lote.criadoEm} />
          </InfoRow>
        </Grid>
      )}
    </CardSecao>
  )
}

function MateriaisConsumidosCard({ ordemId, materialPorId, rastreio }: {
  ordemId: string
  materialPorId: Map<string, Material>
  rastreio?: RastreabilidadeLote
}) {
  const lotesPorConsumo = new Map((rastreio?.origens ?? []).map((o) => [o.consumoId, o.lotes]))
  const { data: consumos = [], isLoading, isError } = useQuery({
    queryKey: ['consumos', ordemId],
    queryFn: () => listarConsumosPorOrdem(ordemId),
  })

  return (
    <CardSecao titulo="Materiais consumidos na produção" icone={<ScienceIcon color="action" fontSize="small" />}>
      {isLoading ? (
        <Skeleton variant="rounded" height={120} />
      ) : isError ? (
        <Alert severity="error">Não foi possível carregar os consumos.</Alert>
      ) : consumos.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
          Nenhum consumo registrado para a ordem de origem.
        </Typography>
      ) : (
        <TableContainer>
          <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
            <TableHead>
              <TableRow>
                <TableCell>Material</TableCell>
                <TableCell align="right">Planejado</TableCell>
                <TableCell align="right">Consumido</TableCell>
                <TableCell align="right">Desvio</TableCell>
                <TableCell>Lotes de origem</TableCell>
                <TableCell>Justificativa</TableCell>
                <TableCell>Registrado por</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {consumos.map((c) => (
                <LinhaConsumo key={c.id} consumo={c} material={materialPorId.get(c.materialId)}
                  lotes={rastreio ? lotesPorConsumo.get(c.id) ?? [] : undefined} />
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </CardSecao>
  )
}

function LinhaConsumo({ consumo: c, material, lotes }: { consumo: ConsumoMaterial; material?: Material; lotes?: LoteOrigem[] }) {
  const desvio = c.desvio ?? 0
  const temDesvio = c.registrado && Math.abs(desvio) > 0.0001
  return (
    <TableRow>
      <TableCell>
        <Tooltip title={material?.descricao ?? ''}>
          <Typography variant="body2" fontWeight={500}>{material?.codigo ?? '…'}</Typography>
        </Tooltip>
      </TableCell>
      <TableCell align="right">
        <Typography variant="body2" noWrap>{formatarQuantidade(c.quantidadePlanejada, c.unidadeDeMedida)}</Typography>
      </TableCell>
      <TableCell align="right">
        <Typography variant="body2" noWrap>
          {c.registrado ? formatarQuantidade(c.quantidadeConsumida, c.unidadeDeMedida) : '—'}
        </Typography>
      </TableCell>
      <TableCell align="right">
        {c.registrado && (
          <Typography variant="body2" noWrap fontWeight={temDesvio ? 600 : 400}
            color={temDesvio ? (desvio > 0 ? 'error.main' : 'warning.main') : 'text.secondary'}>
            {desvio > 0 ? '+' : ''}{formatarQuantidade(desvio)}
          </Typography>
        )}
      </TableCell>
      <TableCell>
        <LotesDeOrigem lotes={lotes} registrado={c.registrado} unidade={c.unidadeDeMedida} />
      </TableCell>
      <TableCell>
        <Typography variant="body2" color={c.justificativa ? 'text.primary' : 'text.disabled'}>
          {c.justificativa ?? '—'}
        </Typography>
      </TableCell>
      <TableCell>
        {c.registrado ? <Assinatura usuario={c.atualizadoPor} instante={c.atualizadoEm} /> : '—'}
      </TableCell>
    </TableRow>
  )
}

/** Links para os lotes de onde saiu o material; compra mostra a NF. */
function LotesDeOrigem({ lotes, registrado, unidade }: { lotes?: LoteOrigem[]; registrado: boolean; unidade: string }) {
  if (!registrado) return <Typography variant="body2" color="text.disabled">—</Typography>
  if (!lotes) return <Skeleton width={120} />
  if (lotes.length === 0) {
    return (
      <Tooltip title="Consumo registrado antes da genealogia de lotes.">
        <Typography variant="body2" color="text.disabled">Sem rastreio</Typography>
      </Tooltip>
    )
  }
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
      {lotes.map((l) => (
        <Box key={l.loteId}>
          <Link component={RouterLink} to={`/lotes/${l.loteId}`} variant="body2"
            sx={{ fontFamily: 'monospace', fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
            {l.numeroLote}
          </Link>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
            {formatarQuantidade(l.quantidade, unidade)}
            {l.origem === 'COMPRA' ? ` · NF ${l.notaFiscal} · ${l.fornecedor}` : ' · produção'}
          </Typography>
        </Box>
      ))}
    </Box>
  )
}

function OndeFoiUsadoCard({ lote, rastreio, carregando, erro, materialPorId }: {
  lote: Lote
  rastreio?: RastreabilidadeLote
  carregando: boolean
  erro: boolean
  materialPorId: Map<string, Material>
}) {
  const destinos = rastreio?.destinos ?? []
  const usado = destinos.reduce((t, d) => t + Number(d.quantidade), 0)

  return (
    <CardSecao titulo="Onde foi usado" icone={<CallSplitIcon color="action" fontSize="small" />}>
      {carregando ? (
        <Skeleton variant="rounded" height={100} />
      ) : erro ? (
        <Alert severity="error">Não foi possível carregar a rastreabilidade do lote.</Alert>
      ) : destinos.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
          {lote.status === 'CONSUMIDO'
            ? 'Lote consumido antes da genealogia de lotes — sem registro de onde foi usado.'
            : 'Este lote ainda não foi usado em nenhuma ordem.'}
        </Typography>
      ) : (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            {`Usado em ${destinos.length} ${destinos.length === 1 ? 'consumo' : 'consumos'} — `
              + `${formatarQuantidade(usado, lote.unidadeDeMedida)} de ${formatarQuantidade(lote.quantidade, lote.unidadeDeMedida)}.`}
          </Typography>
          <TableContainer>
            <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
              <TableHead>
                <TableRow>
                  <TableCell>Ordem</TableCell>
                  <TableCell>Produziu</TableCell>
                  <TableCell align="right">Quantidade</TableCell>
                  <TableCell>Lote gerado</TableCell>
                  <TableCell>Alocado por</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {destinos.map((d) => (
                  <TableRow key={d.alocacaoId}>
                    <TableCell>
                      <Link component={RouterLink} to={`/ordens/${d.ordemProducaoId}`} variant="body2" fontWeight={600} noWrap>
                        {d.ordemCodigo}
                      </Link>
                      <Box sx={{ mt: 0.25 }}><StatusOrdemBadge status={d.ordemStatus} /></Box>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{materialPorId.get(d.materialProduzidoId)?.codigo ?? '…'}</Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" noWrap>{formatarQuantidade(d.quantidade, d.unidadeDeMedida)}</Typography>
                    </TableCell>
                    <TableCell>
                      {d.loteGeradoId ? (
                        <Link component={RouterLink} to={`/lotes/${d.loteGeradoId}`} variant="body2"
                          sx={{ fontFamily: 'monospace', fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
                          {d.loteGeradoNumero}
                        </Link>
                      ) : (
                        <Typography variant="body2" color="text.disabled">Ordem em andamento</Typography>
                      )}
                    </TableCell>
                    <TableCell><Assinatura usuario={d.alocadoPor} instante={d.alocadoEm} /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </CardSecao>
  )
}

function Assinatura({ usuario, instante }: { usuario: string; instante: string }) {
  return (
    <Box>
      <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>{usuario}</Typography>
      <Typography variant="caption" color="text.secondary" noWrap>{formatarDataHora(instante)}</Typography>
    </Box>
  )
}
