import { useState } from 'react'
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
import { formatarDataHora } from '../utils/formatacao'
import type { AcaoAuditoria, EventoAuditoria, MudancaAuditoria, TipoEntidade } from '../types'

const ACAO_LABEL: Record<AcaoAuditoria, string> = {
  CRIADO: 'Criado',
  ALTERADO: 'Alterado',
  STATUS_ALTERADO: 'Status alterado',
  ATIVADA: 'Versão ativada',
  OBSOLETADA: 'Versão obsoletada',
  CONSUMO_REGISTRADO: 'Consumo registrado',
  ORDEM_CONCLUIDA: 'Ordem concluída',
  LOTE_GERADO: 'Lote gerado',
  LOTE_ALOCADO: 'Lote alocado em consumo',
  ENTRADA_REGISTRADA: 'Entrada registrada',
  USUARIO_REGISTRADO: 'Usuário registrado',
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
  lotes: 'Lotes de origem',
  justificativa: 'Justificativa',
  inicioPlanejado: 'Início planejado',
  fimPlanejado: 'Fim planejado',
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
}

/** Rótulos de status e tipos (os eventos guardam as constantes do backend). */
const VALOR_LABEL: Record<string, string> = {
  PLANEJADA: 'Planejada',
  LIBERADA: 'Liberada',
  EM_PRODUCAO: 'Em produção',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
  DISPONIVEL: 'Disponível',
  BLOQUEADO: 'Bloqueado',
  CONSUMIDO: 'Consumido',
  VENCIDO: 'Vencido',
  EM_REVISAO: 'Em revisão',
  ATIVA: 'Ativa',
  OBSOLETA: 'Obsoleta',
  PRODUTO_ACABADO: 'Produto acabado',
  SEMIACABADO: 'Semiacabado',
  MATERIA_PRIMA: 'Matéria-prima',
  PLANEJADOR: 'Planejador',
  GERENTE: 'Gerente',
}

/** Campos numéricos: exibidos no padrão brasileiro (vírgula decimal). */
const CAMPOS_NUMERICOS = new Set(['quantidade', 'quantidadePlanejada', 'quantidadeConsumida', 'quantidadeProduzida', 'desvio', 'saldo'])

/** O JSONB do banco não preserva a ordem das chaves: exibe na ordem de CAMPO_LABEL. */
const ORDEM_CAMPOS = Object.keys(CAMPO_LABEL)
const posicao = (campo: string) => {
  const i = ORDEM_CAMPOS.indexOf(campo)
  return i < 0 ? ORDEM_CAMPOS.length : i
}

const TAMANHO_PAGINA = 20

const ehMudanca = (v: unknown): v is MudancaAuditoria =>
  typeof v === 'object' && v !== null && 'de' in v && 'para' in v

/** Datas ISO viram DD/MM/YYYY; números e constantes (EM_PRODUCAO) viram texto legível. */
function formatarValor(campo: string, valor: unknown): string {
  if (valor == null || valor === '') return '—'
  const texto = String(valor)
  if (CAMPOS_NUMERICOS.has(campo) && !Number.isNaN(Number(texto))) {
    return Number(texto).toLocaleString('pt-BR', { maximumFractionDigits: 4 })
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

function Detalhes({ evento }: { evento: EventoAuditoria }) {
  const entradas = Object.entries(evento.detalhes).sort(([a], [b]) => posicao(a) - posicao(b))
  if (entradas.length === 0) return null
  return (
    <Box component="dl" sx={{ m: 0, mt: 0.5, display: 'grid', gridTemplateColumns: 'auto 1fr', columnGap: 1.5, rowGap: 0.25 }}>
      {entradas.map(([campo, valor]) => (
        <Box key={campo} sx={{ display: 'contents' }}>
          <Typography component="dt" variant="caption" color="text.secondary">
            {CAMPO_LABEL[campo] ?? campo}
          </Typography>
          <Typography component="dd" variant="caption" sx={{ m: 0, overflowWrap: 'anywhere' }}>
            {ehMudanca(valor)
              ? <><s>{formatarValor(campo, valor.de)}</s> → <strong>{formatarValor(campo, valor.para)}</strong></>
              : formatarValor(campo, valor)}
          </Typography>
        </Box>
      ))}
    </Box>
  )
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
  const [tamanho, setTamanho] = useState(TAMANHO_PAGINA)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['auditoria', tipoEntidade, entidadeId, tamanho],
    queryFn: () => consultarEventosAuditoria({ tipoEntidade, entidadeId, tamanho }),
    placeholderData: keepPreviousData,
  })
  const eventos = data?.itens ?? []

  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <HistoryIcon color="action" fontSize="small" />
          <Typography variant="subtitle2">{titulo}</Typography>
          {data && data.total > 0 && (
            <Typography variant="caption" color="text.secondary">({data.total})</Typography>
          )}
        </Box>
        <Divider sx={{ mb: 2 }} />

        {isLoading ? (
          <Skeleton variant="rounded" height={120} />
        ) : isError ? (
          <Alert severity="error">Não foi possível carregar o histórico.</Alert>
        ) : eventos.length === 0 ? (
          <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
            Nenhum evento registrado. Registros criados antes da trilha de auditoria não têm histórico.
          </Typography>
        ) : (
          <Box component="ol" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {eventos.map((evento, i) => (
              <Box component="li" key={evento.id} sx={{ display: 'flex', gap: 1.5 }}>
                {/* Marcador e linha da linha do tempo */}
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', pt: 0.75 }}>
                  <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: 'primary.main', flexShrink: 0 }} />
                  {i < eventos.length - 1 && <Box sx={{ width: 2, flex: 1, bgcolor: 'divider', my: 0.5 }} />}
                </Box>
                <Box sx={{ pb: 2, minWidth: 0, flex: 1 }}>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', columnGap: 1 }}>
                    <Typography variant="body2" fontWeight={600}>{ACAO_LABEL[evento.acao] ?? evento.acao}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {formatarDataHora(evento.ocorridoEm)} · {evento.usuario}
                    </Typography>
                  </Box>
                  <Detalhes evento={evento} />
                </Box>
              </Box>
            ))}
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
