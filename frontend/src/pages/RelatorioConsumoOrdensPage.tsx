import { useEffect, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Link from '@mui/material/Link'
import Skeleton from '@mui/material/Skeleton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import AssessmentIcon from '@mui/icons-material/AssessmentOutlined'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { consultarConsumoDeOrdens } from '../api/relatorios'
import { EmptyState } from '../components/EmptyState'
import { PageHeader } from '../components/PageHeader'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { CamposDePeriodo, ehDataIso, periodoInvertido } from '../components/consulta/CamposDePeriodo'
import { CelulaDeCabecalho, ResumoDaTabela, useTabelaInterativa, type Coluna } from '../components/consulta/TabelaInterativa'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { usePermissoes } from '../hooks/usePermissoes'
import { formatarCodigoMaterial, formatarData, formatarQuantidade } from '../utils/formatacao'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import type { LinhaDeConsumoDeOrdem } from '../types'

/** Mesmo limite do servidor (366 dias, inclusive); checar aqui evita ir ao servidor só para ouvir 422. */
const MAXIMO_DE_DIAS = 366
const DIA_EM_MS = 24 * 60 * 60 * 1000

export const periodoLongoDemais = (de: string, ate: string) =>
  ehDataIso(de) && ehDataIso(ate) && (Date.parse(ate) - Date.parse(de)) / DIA_EM_MS >= MAXIMO_DE_DIAS

const colunas: Coluna<LinhaDeConsumoDeOrdem>[] = [
  { chave: 'ordem', rotulo: 'Ordem', valor: (l) => l.ordemCodigo },
  { chave: 'fim', rotulo: 'Fim planejado', valor: (l) => l.fimPlanejado, texto: (l) => formatarData(l.fimPlanejado) },
  {
    chave: 'produto', rotulo: 'Produto', valor: (l) => l.produtoDescricao,
    texto: (l) => `${formatarCodigoMaterial(l.produtoCodigo)} ${l.produtoDescricao}`,
  },
  { chave: 'componente', rotulo: 'Componente', valor: (l) => l.componenteCodigo, texto: (l) => formatarCodigoMaterial(l.componenteCodigo) },
  { chave: 'descricao', rotulo: 'Descrição', valor: (l) => l.componenteDescricao },
  { chave: 'planejado', rotulo: 'Planejado', valor: (l) => l.quantidadePlanejada, texto: (l) => formatarQuantidade(l.quantidadePlanejada, l.unidadeDeMedida) },
  { chave: 'consumido', rotulo: 'Consumido', valor: (l) => l.quantidadeConsumida, texto: (l) => formatarQuantidade(l.quantidadeConsumida, l.unidadeDeMedida) },
  { chave: 'desvio', rotulo: 'Desvio', valor: (l) => l.desvio, texto: (l) => formatarQuantidade(l.desvio, l.unidadeDeMedida) },
]

/**
 * Relatório de consumo de ordens: para as ordens CONCLUÍDAS com fim planejado no período, o consumo total de cada
 * componente (a soma dos lotes). A ordem se repete em cada linha de componente; o detalhe por lote está na ordem.
 */
export function RelatorioConsumoOrdensPage() {
  const url = useParametrosDaUrl()
  const { tem } = usePermissoes()
  const deAplicado = ehDataIso(url.texto('de')) ? url.texto('de') : ''
  const ateAplicado = ehDataIso(url.texto('ate')) ? url.texto('ate') : ''
  const periodoCompleto = deAplicado !== '' && ateAplicado !== ''
  const periodoValido = periodoCompleto && !periodoInvertido(deAplicado, ateAplicado) && !periodoLongoDemais(deAplicado, ateAplicado)

  // Os campos editam um rascunho; o relatório só muda ao clicar em Pesquisar.
  const [de, setDe] = useState(deAplicado)
  const [ate, setAte] = useState(ateAplicado)
  useEffect(() => { setDe(deAplicado); setAte(ateAplicado) }, [deAplicado, ateAplicado])

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['relatorios', 'consumo-de-ordens', deAplicado, ateAplicado],
    queryFn: () => consultarConsumoDeOrdens(deAplicado, ateAplicado),
    enabled: periodoValido,
    placeholderData: keepPreviousData,
  })
  const linhas = data?.linhas ?? []
  const tabela = useTabelaInterativa(linhas, colunas)

  const mensagemDoRascunho = periodoInvertido(de, ate)
    ? 'O fim do período não pode ser anterior ao início.'
    : periodoLongoDemais(de, ate) ? `O período pode ter no máximo ${MAXIMO_DE_DIAS} dias.` : null
  const pesquisar = () => {
    if (!de || !ate || mensagemDoRascunho) return
    url.atualizar({ de, ate })
  }
  const limpar = () => { setDe(''); setAte(''); url.atualizar({ de: undefined, ate: undefined }) }

  return (
    <Box>
      <PageHeader
        titulo="Consumo de ordens"
        subtitulo={periodoValido && data
          ? `${data.linhas.length} ${data.linhas.length === 1 ? 'linha' : 'linhas'} de ${data.totalDeOrdens} ${data.totalDeOrdens === 1 ? 'ordem concluída' : 'ordens concluídas'}`
          : 'Consumo total por componente das ordens concluídas no período'}
        breadcrumbs={[{ label: 'Relatórios' }, { label: 'Consumo de ordens' }]}
      />

      <PainelDeConsulta titulo="Período" onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposDePeriodo rotuloDe="Fim planejado de" de={de} onDe={setDe} ate={ate} onAte={setAte} />
      </PainelDeConsulta>

      {mensagemDoRascunho && <Alert severity="warning" sx={{ mb: 2 }}>{mensagemDoRascunho}</Alert>}
      {periodoCompleto && !periodoValido && !mensagemDoRascunho && (
        <Alert severity="warning" sx={{ mb: 2 }}>Informe um período válido (até {MAXIMO_DE_DIAS} dias, com o fim depois do início).</Alert>
      )}
      {isError && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(error, 'Não foi possível gerar o relatório.')}</Alert>}

      <Card>
        {!periodoValido ? (
          <EmptyState
            Icone={AssessmentIcon}
            titulo="Escolha o período"
            descricao="Informe o início e o fim do período do fim planejado das ordens e clique em Pesquisar."
          />
        ) : isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : linhas.length === 0 ? (
          <EmptyState
            Icone={AssessmentIcon}
            titulo="Nenhuma ordem concluída neste período"
            descricao="Só entram ordens concluídas com o fim planejado entre as datas escolhidas."
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={linhas.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
            <TableContainer>
              <Table size="small">
                <TableHead sx={{ '& th': { whiteSpace: 'nowrap' } }}>
                  <TableRow>
                    <CelulaDeCabecalho chave="ordem" rotulo="Ordem" controle={tabela.controle} />
                    <CelulaDeCabecalho chave="fim" rotulo="Fim planejado" controle={tabela.controle} />
                    <CelulaDeCabecalho chave="produto" rotulo="Produto" controle={tabela.controle} />
                    <CelulaDeCabecalho chave="componente" rotulo="Componente" controle={tabela.controle} />
                    <CelulaDeCabecalho chave="descricao" rotulo="Descrição" controle={tabela.controle} />
                    <CelulaDeCabecalho chave="planejado" rotulo="Planejado" controle={tabela.controle} align="right" />
                    <CelulaDeCabecalho chave="consumido" rotulo="Consumido" controle={tabela.controle} align="right" />
                    <CelulaDeCabecalho chave="desvio" rotulo="Desvio" controle={tabela.controle} align="right" />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tabela.linhas.map((l) => (
                    <TableRow key={`${l.ordemId}-${l.componenteCodigo}`} hover>
                      <TableCell>
                        {tem('ORDEM_VISUALIZAR') ? (
                          <Link component={RouterLink} to={`/ordens/${l.ordemId}`} underline="hover" variant="body2" noWrap sx={{ fontWeight: 600 }}>
                            {l.ordemCodigo}
                          </Link>
                        ) : (
                          <Typography variant="body2" noWrap sx={{ fontWeight: 600 }}>{l.ordemCodigo}</Typography>
                        )}
                      </TableCell>
                      <TableCell><Typography variant="body2" noWrap sx={{ color: 'text.secondary' }}>{formatarData(l.fimPlanejado)}</Typography></TableCell>
                      <TableCell>
                        <Tooltip title={l.produtoDescricao}>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace' }} noWrap>{formatarCodigoMaterial(l.produtoCodigo)}</Typography>
                        </Tooltip>
                      </TableCell>
                      <TableCell><Typography variant="body2" sx={{ fontFamily: 'monospace' }} noWrap>{formatarCodigoMaterial(l.componenteCodigo)}</Typography></TableCell>
                      <TableCell sx={{ minWidth: 160 }}><Typography variant="body2">{l.componenteDescricao}</Typography></TableCell>
                      <TableCell align="right"><Typography variant="body2">{formatarQuantidade(l.quantidadePlanejada, l.unidadeDeMedida)}</Typography></TableCell>
                      <TableCell align="right"><Typography variant="body2" sx={{ fontWeight: 600 }}>{formatarQuantidade(l.quantidadeConsumida, l.unidadeDeMedida)}</Typography></TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" sx={{ color: corDoDesvio(l.desvio) }}>{formatarQuantidade(l.desvio, l.unidadeDeMedida)}</Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <Typography
              variant="caption"
              sx={{ color: 'text.secondary', display: 'block', p: 2 }}>
              Consumo total por componente (a soma dos lotes). Para ver de quais lotes saiu, abra a ordem.
            </Typography>
          </Box>
        )}
      </Card>
    </Box>
  )
}

/** Gastou mais que o planejado = vermelho; menos = laranja; igual ou sem dado = cor padrão. */
function corDoDesvio(desvio: number | null) {
  if (desvio == null || desvio === 0) return 'text.secondary'
  return desvio > 0 ? 'error.main' : 'warning.main'
}
