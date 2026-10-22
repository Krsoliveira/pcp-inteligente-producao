import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Skeleton from '@mui/material/Skeleton'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongOutlined'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pesquisarNotasFiscais } from '../api/notasFiscais'
import { listarMateriais } from '../api/materiais'
import { NotaFiscalTable } from '../components/suprimentos/NotaFiscalTable'
import { CamposFiltroNotas, ehDataIso, periodoInvertido } from '../components/suprimentos/CamposFiltroNotas'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { EmptyState } from '../components/EmptyState'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { SubfiltroTabela, useSubfiltro } from '../components/consulta/SubfiltroTabela'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { formatarCodigoMaterial } from '../utils/formatacao'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'

/** Segunda etapa: o resultado da consulta de notas, paginado no servidor, com subfiltro na página. */
export function NotasFiscaisResultadosPage() {
  const url = useParametrosDaUrl()
  const buscaAplicada = url.texto('busca')
  const deAplicado = ehDataIso(url.texto('de')) ? url.texto('de') : ''
  const ateAplicado = ehDataIso(url.texto('ate')) ? url.texto('ate') : ''
  const periodoValido = !periodoInvertido(deAplicado, ateAplicado)
  const pagina = url.numero('pagina', 0)
  const tamanho = url.numero('tamanho', 25)

  const [busca, setBusca] = useState(buscaAplicada)
  const [de, setDe] = useState(deAplicado)
  const [ate, setAte] = useState(ateAplicado)
  useEffect(() => { setBusca(buscaAplicada); setDe(deAplicado); setAte(ateAplicado) }, [buscaAplicada, deAplicado, ateAplicado])

  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['notas-fiscais', 'pesquisa', buscaAplicada, deAplicado, ateAplicado, pagina, tamanho],
    queryFn: () => pesquisarNotasFiscais({
      busca: buscaAplicada || undefined, de: deAplicado || undefined, ate: ateAplicado || undefined, pagina, tamanho,
    }),
    enabled: periodoValido,
    placeholderData: keepPreviousData,
  })
  const notas = data?.itens ?? []
  const total = data?.total ?? 0
  const subfiltro = useSubfiltro(notas, (n) => [
    n.numero, n.fornecedor, ...n.itens.map((l) => l.numeroLote),
    ...n.itens.map((l) => formatarCodigoMaterial(materialPorId.get(l.materialId)?.codigo)),
  ].join(' '))
  const filtrando = buscaAplicada !== '' || deAplicado !== '' || ateAplicado !== ''

  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      url.atualizar({ pagina: Math.max(0, data.totalPaginas - 1) })
    }
  }, [data, pagina]) // eslint-disable-line react-hooks/exhaustive-deps

  const pesquisar = () => {
    if (periodoInvertido(de, ate)) return
    url.atualizar({ busca: busca.trim(), de, ate, pagina: undefined })
  }
  const limpar = () => {
    setBusca(''); setDe(''); setAte('')
    url.atualizar({ busca: undefined, de: undefined, ate: undefined, pagina: undefined })
  }

  return (
    <Box>
      <PageHeader
        titulo="Entrada de notas"
        subtitulo={isLoading ? '…' : `${total} ${total === 1 ? 'nota fiscal' : 'notas fiscais'}${filtrando ? ' encontradas' : ' registradas'}`}
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Entrada de notas', href: '/notas-fiscais' }, { label: 'Resultados' }]}
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroNotas busca={busca} onBusca={setBusca} de={de} onDe={setDe} ate={ate} onAte={setAte} />
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
            descricao={filtrando ? 'Ajuste o termo ou o período.' : undefined}
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <SubfiltroTabela valor={subfiltro.texto} onChange={subfiltro.setTexto} exibidas={subfiltro.filtrados.length} naPagina={notas.length} />
            <NotaFiscalTable notas={subfiltro.filtrados} materialPorId={materialPorId} />
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
