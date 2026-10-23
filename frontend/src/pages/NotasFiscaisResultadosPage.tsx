import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Skeleton from '@mui/material/Skeleton'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongOutlined'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pesquisarNotasFiscais } from '../api/notasFiscais'
import { listarMateriais } from '../api/materiais'
import { NotaFiscalTable, colunasDeNotas } from '../components/suprimentos/NotaFiscalTable'
import { CamposFiltroNotas } from '../components/suprimentos/CamposFiltroNotas'
import { ehDataIso, periodoInvertido } from '../components/consulta/CamposDePeriodo'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { EmptyState } from '../components/EmptyState'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'

/** Segunda etapa: o resultado da consulta de notas, paginado no servidor, com filtro e ordenação por coluna na página. */
export function NotasFiscaisResultadosPage() {
  const url = useParametrosDaUrl()
  const deAplicado = ehDataIso(url.texto('de')) ? url.texto('de') : ''
  const ateAplicado = ehDataIso(url.texto('ate')) ? url.texto('ate') : ''
  const periodoValido = !periodoInvertido(deAplicado, ateAplicado)
  const pagina = url.numero('pagina', 0)
  const tamanho = url.numero('tamanho', 25)

  const [de, setDe] = useState(deAplicado)
  const [ate, setAte] = useState(ateAplicado)
  useEffect(() => { setDe(deAplicado); setAte(ateAplicado) }, [deAplicado, ateAplicado])

  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['notas-fiscais', 'pesquisa', deAplicado, ateAplicado, pagina, tamanho],
    queryFn: () => pesquisarNotasFiscais({
      de: deAplicado || undefined, ate: ateAplicado || undefined, pagina, tamanho,
    }),
    enabled: periodoValido,
    placeholderData: keepPreviousData,
  })
  const notas = data?.itens ?? []
  const total = data?.total ?? 0
  const tabela = useTabelaInterativa(notas, colunasDeNotas(materialPorId))
  const filtrando = deAplicado !== '' || ateAplicado !== ''

  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      url.atualizar({ pagina: Math.max(0, data.totalPaginas - 1) })
    }
  }, [data, pagina]) // eslint-disable-line react-hooks/exhaustive-deps

  const pesquisar = () => {
    if (periodoInvertido(de, ate)) return
    url.atualizar({ de, ate, pagina: undefined })
  }
  const limpar = () => {
    setDe(''); setAte('')
    url.atualizar({ de: undefined, ate: undefined, pagina: undefined })
  }

  return (
    <Box>
      <PageHeader
        titulo="Entrada de notas"
        subtitulo={isLoading ? '…' : `${total} ${total === 1 ? 'nota fiscal' : 'notas fiscais'}${filtrando ? ' encontradas' : ' registradas'}`}
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Entrada de notas', href: '/notas-fiscais' }, { label: 'Resultados' }]}
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroNotas de={de} onDe={setDe} ate={ate} onAte={setAte} />
      </PainelDeConsulta>

      {!periodoValido && <Alert severity="warning" sx={{ mb: 2 }}>O fim do período não pode ser anterior ao início.</Alert>}
      {isError && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(error, 'Não foi possível carregar as notas fiscais.')}</Alert>}

      <Card>
        {isLoading && periodoValido ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : notas.length === 0 ? (
          <EmptyState
            Icone={ReceiptLongIcon}
            titulo={filtrando ? 'Nenhuma nota fiscal encontrada com os filtros' : 'Nenhuma nota fiscal registrada'}
            descricao={filtrando ? 'Ajuste o período.' : undefined}
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={notas.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
            <NotaFiscalTable notas={tabela.linhas} materialPorId={materialPorId} controle={tabela.controle} />
            <PaginacaoTabela
              total={total}
              pagina={pagina}
              tamanho={tamanho}
              onPagina={(nova) => url.atualizar({ pagina: nova })}
              onTamanho={(novo) => url.atualizar({ tamanho: novo, pagina: undefined })}
            />
          </Box>
        )}
      </Card>
    </Box>
  )
}
