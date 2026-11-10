import { useState } from 'react'
import { usePermissoes } from '../hooks/usePermissoes'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Divider from '@mui/material/Divider'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Typography from '@mui/material/Typography'
import HistoryIcon from '@mui/icons-material/History'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { consultarEventosAuditoria } from '../api/auditoria'
import { formatarCodigoMaterial, formatarDataHora, formatarQuantidade } from '../utils/formatacao'
import { ROTULOS_DE_STATUS, StatusListaBadge, StatusLoteBadge, StatusOrdemBadge } from './StatusBadge'
import { TOM_VIVO } from './tonsDosGraficos'
import ArrowIcon from '@mui/icons-material/ArrowRightAltOutlined'
import {
  TIPO_MATERIAL_LABEL,
  type AcaoAuditoria,
  type EventoAuditoria,
  type MudancaAuditoria,
  type StatusListaTecnica,
  type StatusLote,
  type StatusOrdem,
  type TipoEntidade,
} from '../types'

const ACAO_LABEL: Record<AcaoAuditoria, string> = {
  CRIADO: 'Criado',
  ALTERADO: 'Alterado',
  STATUS_ALTERADO: 'Status alterado',
  ATIVADA: 'Versão ativada',
  OBSOLETADA: 'Versão obsoletada',
  CONSUMO_REGISTRADO: 'Consumo registrado',
  CONSUMO_ADICIONADO: 'Item adicionado ao consumo',
  ORDEM_CONCLUIDA: 'Ordem concluída',
  LOTE_GERADO: 'Lote gerado',
  LOTE_ALOCADO: 'Lote alocado em consumo',
  ENTRADA_REGISTRADA: 'Entrada registrada',
  USUARIO_REGISTRADO: 'Usuário registrado',
  EXCLUIDO: 'Excluído',
  PERFIL_ATRIBUIDO: 'Perfil atribuído',
  SENHA_ALTERADA: 'Senha alterada',
  SENHA_REDEFINIDA: 'Senha redefinida',
  USUARIO_DESATIVADO: 'Usuário desativado',
  USUARIO_REATIVADO: 'Usuário reativado',
}

const CAMPO_LABEL: Record<string, string> = {
  codigo: 'Código',
  descricao: 'Descrição',
  tipo: 'Tipo',
  unidadeDeMedida: 'Unidade',
  material: 'Material',
  versao: 'Versão',
  versaoAnterior: 'Versão anterior',
  substituidaPor: 'Substituída por',
  versaoListaTecnica: 'Lista técnica',
  itens: 'Itens',
  status: 'Status',
  centroDeTrabalho: 'Centro de trabalho',
  quantidade: 'Quantidade',
  quantidadePlanejada: 'Planejado',
  quantidadeConsumida: 'Consumido',
  quantidadeProduzida: 'Produzido',
  desvio: 'Desvio',
  saldo: 'Saldo',
  lotes: 'Lotes',
  justificativa: 'Justificativa',
  inicioPlanejado: 'Início planejado',
  fimPlanejado: 'Fim planejado',
  consumosProjetados: 'Consumos projetados',
  naoPlanejado: 'Fora da lista',
  tipoOrdem: 'Tipo de ordem',
  loteGerado: 'Lote gerado',
  numeroLote: 'Lote',
  ordemProducao: 'Ordem',
  fornecedor: 'Fornecedor',
  notaFiscal: 'Nota fiscal',
  dataEmissaoNf: 'Emissão da NF',
  dataRecebimento: 'Recebimento',
  dataFabricacao: 'Fabricação',
  dataValidade: 'Validade',
  nome: 'Nome',
  cor: 'Cor',
  perfil: 'Perfil',
  motivo: 'Motivo',
  email: 'E-mail',
  permissoes: 'Permissões',
  permissoesAdicionadas: 'Permissões adicionadas',
  permissoesRemovidas: 'Permissões removidas',
  eraProvisoria: 'Era provisória',
}

/** Rótulos de status e tipos (os eventos guardam as constantes do backend). */
const VALOR_LABEL: Record<string, string> = { ...ROTULOS_DE_STATUS, ...TIPO_MATERIAL_LABEL }

/** Campos numéricos: exibidos no padrão brasileiro (vírgula decimal). */
const CAMPOS_NUMERICOS = new Set(['quantidade', 'quantidadePlanejada', 'quantidadeConsumida', 'quantidadeProduzida', 'desvio', 'saldo'])

/** O JSONB do banco não preserva a ordem das chaves: exibe na ordem de CAMPO_LABEL. */
const ORDEM_CAMPOS = Object.keys(CAMPO_LABEL)
const posicao = (campo: string) => {
  const i = ORDEM_CAMPOS.indexOf(campo)
  return i < 0 ? ORDEM_CAMPOS.length : i
}

const TAMANHO_PAGINA = 20

type Tom = 'neutro' | 'informacao' | 'atencao' | 'sucesso' | 'erro'

/** Ponto (tom vivo), fundo tingido e texto escuro de cada tom: os mesmos do selo de status (design system, "Status"). */
const TOM: Record<Tom, { ponto: string; fundo: string; texto: string }> = {
  neutro: { ponto: TOM_VIVO.neutro, fundo: 'rgba(148,163,184,0.12)', texto: '#475569' },
  informacao: { ponto: TOM_VIVO.informacao, fundo: 'rgba(37,99,235,0.10)', texto: '#1D4ED8' },
  atencao: { ponto: TOM_VIVO.atencao, fundo: 'rgba(217,119,6,0.10)', texto: '#B45309' },
  sucesso: { ponto: TOM_VIVO.sucesso, fundo: 'rgba(5,150,105,0.10)', texto: '#047857' },
  erro: { ponto: TOM_VIVO.erro, fundo: 'rgba(220,38,38,0.10)', texto: '#B91C1C' },
}

const TOM_DA_ACAO: Record<AcaoAuditoria, Tom> = {
  CRIADO: 'informacao',
  ALTERADO: 'informacao',
  STATUS_ALTERADO: 'informacao',
  ATIVADA: 'sucesso',
  OBSOLETADA: 'neutro',
  CONSUMO_REGISTRADO: 'informacao',
  CONSUMO_ADICIONADO: 'atencao',
  ORDEM_CONCLUIDA: 'sucesso',
  LOTE_GERADO: 'sucesso',
  LOTE_ALOCADO: 'informacao',
  ENTRADA_REGISTRADA: 'sucesso',
  USUARIO_REGISTRADO: 'informacao',
  EXCLUIDO: 'erro',
  PERFIL_ATRIBUIDO: 'informacao',
  SENHA_ALTERADA: 'neutro',
  SENHA_REDEFINIDA: 'atencao',
  USUARIO_DESATIVADO: 'erro',
  USUARIO_REATIVADO: 'sucesso',
}

/** Tom do status novo, igual ao do selo (StatusBadge), para o evento de mudança de status ter a cor de onde chegou. */
const TOM_DO_STATUS: Record<string, Tom> = {
  PLANEJADA: 'neutro', LIBERADA: 'informacao', EM_PRODUCAO: 'atencao', CONCLUIDA: 'sucesso', CANCELADA: 'erro',
  EM_REVISAO: 'atencao', ATIVA: 'sucesso', OBSOLETA: 'neutro',
  DISPONIVEL: 'sucesso', BLOQUEADO: 'atencao', CONSUMIDO: 'neutro', VENCIDO: 'erro',
}

const STATUS_DE_ORDEM = new Set<string>(['PLANEJADA', 'LIBERADA', 'EM_PRODUCAO', 'CONCLUIDA', 'CANCELADA'])
const STATUS_DE_LISTA = new Set<string>(['EM_REVISAO', 'ATIVA', 'OBSOLETA'])
const STATUS_DE_LOTE = new Set<string>(['DISPONIVEL', 'BLOQUEADO', 'CONSUMIDO', 'VENCIDO'])

/** Campos que ocupam a linha inteira: texto longo ou lista de itens. */
const CAMPOS_DE_LINHA_INTEIRA = new Set(['justificativa', 'lotes', 'permissoes', 'permissoesAdicionadas', 'permissoesRemovidas', 'itens'])

/** "consumosProjetados" (sem rótulo cadastrado) vira "Consumos projetados". */
const rotuloDoCampo = (campo: string) => {
  const texto = campo.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase()
  return CAMPO_LABEL[campo] ?? texto.charAt(0).toUpperCase() + texto.slice(1)
}

const ehMudanca = (v: unknown): v is MudancaAuditoria =>
  typeof v === 'object' && v !== null && 'de' in v && 'para' in v

/** Datas ISO viram DD/MM/YYYY; números e constantes (EM_PRODUCAO) viram texto legível. */
const CAMPOS_DE_PERMISSOES = new Set(['permissoes', 'permissoesAdicionadas', 'permissoesRemovidas'])

/** "ORDEM_CRIAR" vira "Ordem criar". */
const constanteLegivel = (constante: string) => {
  const texto = constante.replace(/_/g, ' ').toLowerCase()
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

function formatarValor(campo: string, valor: unknown, unidade?: string): string {
  if (valor == null || valor === '') return '—'
  if (typeof valor === 'boolean') return valor ? 'Sim' : 'Não'
  const texto = String(valor)
  if (campo === 'material') return formatarCodigoMaterial(texto)
  if (CAMPOS_DE_PERMISSOES.has(campo)) return texto.split(',').map((p) => constanteLegivel(p.trim())).join(', ')
  if (CAMPOS_NUMERICOS.has(campo) && !Number.isNaN(Number(texto))) {
    const numero = Number(texto)
    return `${campo === 'desvio' && numero > 0 ? '+' : ''}${formatarQuantidade(numero, unidade)}`
  }
  const data = /^(\d{4})-(\d{2})-(\d{2})$/.exec(texto)
  if (data) return `${data[3]}/${data[2]}/${data[1]}`
  if (VALOR_LABEL[texto]) return VALOR_LABEL[texto]
  if (/^[A-Z][A-Z_]+$/.test(texto)) {
    const legivel = texto.replace(/_/g, ' ').toLowerCase()
    return legivel.charAt(0).toUpperCase() + legivel.slice(1)
  }
  return texto
}

function SeloDeStatus({ valor }: { valor: string }) {
  if (STATUS_DE_ORDEM.has(valor)) return <StatusOrdemBadge status={valor as StatusOrdem} />
  if (STATUS_DE_LISTA.has(valor)) return <StatusListaBadge status={valor as StatusListaTecnica} />
  if (STATUS_DE_LOTE.has(valor)) return <StatusLoteBadge status={valor as StatusLote} />
  return <>{formatarValor('status', valor)}</>
}

/** "14: 80; 7: 20" (lote: quantidade) vira uma etiqueta por lote. */
function EtiquetasDeLotes({ resumo, unidade }: { resumo: string; unidade?: string }) {
  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
      {resumo.split(';').map((parte) => parte.trim()).filter(Boolean).map((parte) => {
        const [numero, quantidade] = parte.split(':').map((p) => p.trim())
        const texto = quantidade === undefined
          ? parte
          : `Lote ${numero} · ${formatarQuantidade(Number(quantidade), unidade)}`
        return (
          <Box key={parte} component="span" sx={{
            px: 1, py: 0.25, borderRadius: 99, border: 1, borderColor: 'divider', bgcolor: 'background.default',
            fontSize: '0.75rem', fontWeight: 500, whiteSpace: 'nowrap',
          }}>
            {texto}
          </Box>
        )
      })}
    </Box>
  )
}

function Valor({ campo, valor, unidade }: { campo: string; valor: unknown; unidade?: string }) {
  if (campo === 'status' && ehMudanca(valor)) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
        <Box sx={{ opacity: 0.7 }}><SeloDeStatus valor={String(valor.de)} /></Box>
        <ArrowIcon fontSize="small" sx={{ color: 'text.secondary' }} />
        <SeloDeStatus valor={String(valor.para)} />
      </Box>
    )
  }
  if (campo === 'status' && typeof valor === 'string') return <SeloDeStatus valor={valor} />
  if (ehMudanca(valor)) {
    return (
      <>
        <Box component="s" sx={{ color: 'text.secondary' }}>{formatarValor(campo, valor.de, unidade)}</Box>{' → '}
        <strong>{formatarValor(campo, valor.para, unidade)}</strong>
      </>
    )
  }
  if (campo === 'lotes' && typeof valor === 'string') return <EtiquetasDeLotes resumo={valor} unidade={unidade} />
  const desviado = campo === 'desvio' && Number(valor) !== 0
  return (
    <Box component="span" sx={desviado ? { color: TOM.atencao.texto, fontWeight: 700 } : undefined}>
      {formatarValor(campo, valor, unidade)}
    </Box>
  )
}

function Detalhes({ evento }: { evento: EventoAuditoria }) {
  const unidade = typeof evento.detalhes.unidadeDeMedida === 'string' ? evento.detalhes.unidadeDeMedida : undefined
  const temQuantidade = Object.keys(evento.detalhes).some((campo) => CAMPOS_NUMERICOS.has(campo))
  // Com quantidade no evento, a unidade vai junto do número ("80 kg") e não ganha uma célula própria; a mudança
  // de status aparece no cabeçalho do evento, ao lado do título.
  const entradas = Object.entries(evento.detalhes)
    .filter(([campo]) => !(temQuantidade && campo === 'unidadeDeMedida'))
    .filter(([campo, valor]) => !(campo === 'status' && ehMudanca(valor)))
    .sort(([a], [b]) => posicao(a) - posicao(b))
  if (entradas.length === 0) return null
  return (
    <Box component="dl" sx={{
      m: 0, mt: 1.5, display: 'grid', gap: 1.5, columnGap: 3,
      gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))',
    }}>
      {entradas.map(([campo, valor]) => (
        <Box key={campo} sx={{ minWidth: 0, ...(CAMPOS_DE_LINHA_INTEIRA.has(campo) ? { gridColumn: '1 / -1' } : {}) }}>
          <Typography component="dt" variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
            {rotuloDoCampo(campo)}
          </Typography>
          <Typography component="dd" variant="body2" sx={{ m: 0, fontWeight: 500, overflowWrap: 'anywhere', fontVariantNumeric: 'tabular-nums' }}>
            <Valor campo={campo} valor={valor} unidade={temQuantidade ? unidade : undefined} />
          </Typography>
        </Box>
      ))}
    </Box>
  )
}

/** Cor do evento: a do status de chegada numa mudança de status; consumo com desvio chama atenção; senão, a da ação. */
function tomDoEvento(evento: EventoAuditoria): Tom {
  const status = evento.detalhes.status
  const statusNovo = ehMudanca(status) ? String(status.para) : undefined
  if (evento.acao === 'STATUS_ALTERADO' && statusNovo && TOM_DO_STATUS[statusNovo]) return TOM_DO_STATUS[statusNovo]
  const desvio = evento.detalhes.desvio
  if (evento.acao.startsWith('CONSUMO') && desvio != null && Number(desvio) !== 0) return 'atencao'
  return TOM_DA_ACAO[evento.acao] ?? 'neutro'
}

interface HistoricoAuditoriaProps {
  tipoEntidade: TipoEntidade
  entidadeId: string
  titulo?: string
}

/**
 * Linha do tempo da trilha de auditoria de um registro (mais recente primeiro):
 * o que aconteceu, quem fez, quando e o que mudou.
 */
export function HistoricoAuditoria({ tipoEntidade, entidadeId, titulo = 'Histórico' }: HistoricoAuditoriaProps) {
  const { tem } = usePermissoes()
  const [tamanho, setTamanho] = useState(TAMANHO_PAGINA)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['auditoria', tipoEntidade, entidadeId, tamanho],
    queryFn: () => consultarEventosAuditoria({ tipoEntidade, entidadeId, tamanho }),
    placeholderData: keepPreviousData,
    enabled: tem('AUDITORIA_VISUALIZAR'),
  })
  const eventos = data?.itens ?? []

  // Sem a permissão o servidor recusa a consulta (403); o bloco nem aparece.
  if (!tem('AUDITORIA_VISUALIZAR')) return null

  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <HistoryIcon color="action" fontSize="small" />
          <Typography variant="subtitle2">{titulo}</Typography>
          {data && data.total > 0 && (
            <Typography variant="caption" sx={{ color: 'text.secondary' }}>({data.total})</Typography>
          )}
        </Box>
        <Divider sx={{ mb: 2 }} />

        {isLoading ? (
          <Skeleton variant="rounded" height={120} />
        ) : isError ? (
          <Alert severity="error">Não foi possível carregar o histórico.</Alert>
        ) : eventos.length === 0 ? (
          <Typography
            variant="body2"
            sx={{ color: 'text.secondary', py: 2, textAlign: 'center' }}>
            Nenhum evento registrado. Registros criados antes da trilha de auditoria não têm histórico.
          </Typography>
        ) : (
          <Box component="ol" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {eventos.map((evento, i) => {
              const nomeDoTom = tomDoEvento(evento)
              const tom = TOM[nomeDoTom]
              return (
                <Box component="li" key={evento.id} data-tom={nomeDoTom} sx={{ display: 'flex', gap: 1.5 }}>
                  {/* Marcador e linha da linha do tempo, na cor do evento */}
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', pt: 1.5 }}>
                    <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: tom.ponto, boxShadow: `0 0 0 3px ${tom.fundo}`, flexShrink: 0 }} />
                    {i < eventos.length - 1 && <Box sx={{ width: 2, flex: 1, bgcolor: 'divider', my: 0.5 }} />}
                  </Box>
                  <Box sx={{ pb: 2, minWidth: 0, flex: 1 }}>
                    <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, px: 2, py: 1.5 }}>
                      <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', columnGap: 2, rowGap: 0.5 }}>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', columnGap: 1.5, rowGap: 0.5 }}>
                          <Box component="span" sx={{
                            px: 1.25, py: 0.25, borderRadius: 99, bgcolor: tom.fundo, color: tom.texto, fontSize: '0.8rem', fontWeight: 600,
                          }}>
                            {ACAO_LABEL[evento.acao] ?? evento.acao}
                          </Box>
                          {ehMudanca(evento.detalhes.status) && <Valor campo="status" valor={evento.detalhes.status} />}
                        </Box>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                          {formatarDataHora(evento.ocorridoEm)} · {evento.usuario}
                        </Typography>
                      </Box>
                      <Detalhes evento={evento} />
                    </Box>
                  </Box>
                </Box>
              )
            })}
          </Box>
        )}

        {data && data.total > eventos.length && tamanho < 200 && (
          <Box sx={{ textAlign: 'center' }}>
            <Button size="small" disabled={isFetching} onClick={() => setTamanho((t) => Math.min(t + TAMANHO_PAGINA, 200))}>
              {isFetching ? 'Carregando…' : `Ver mais (${data.total - eventos.length})`}
            </Button>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}
