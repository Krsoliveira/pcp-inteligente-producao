import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Chip from '@mui/material/Chip'
import Skeleton from '@mui/material/Skeleton'
import ListAltIcon from '@mui/icons-material/ListAlt'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pesquisarOrdens } from '../api/ordens'
import { OrdemTable, colunasDeOrdens } from '../components/OrdemTable'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { EmptyState } from '../components/EmptyState'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { CamposFiltroOrdens, STATUS_VALIDOS } from '../components/ordens/CamposFiltroOrdens'
import { ehDataIso, periodoInvertido } from '../components/consulta/CamposDePeriodo'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { formatarData } from '../utils/formatacao'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import type { OrdemProducao, StatusOrdem } from '../types'

/** Texto do chip do filtro por fim planejado (um dos lados pode faltar). */
function rotuloDoFim(de: string, ate: string) {
  if (de && ate) return `Fim planejado: ${formatarData(de)} a ${formatarData(ate)}`
  return de ? `Fim planejado a partir de ${formatarData(de)}` : `Fim planejado até ${formatarData(ate)}`
}

/** Segunda etapa: o resultado da consulta, paginado no servidor, com filtro e ordenação por coluna na página. */
export function OrdensResultadosPage() {
  const url = useParametrosDaUrl()
  const buscaAplicada = url.texto('busca')
  const statusBruto = url.texto('status') as StatusOrdem
  const statusAplicado = STATUS_VALIDOS.includes(statusBruto) ? statusBruto : undefined
  const deAplicado = ehDataIso(url.texto('de')) ? url.texto('de') : ''
  const ateAplicado = ehDataIso(url.texto('ate')) ? url.texto('ate') : ''
  // Vêm dos atalhos do dashboard; não têm campo no formulário, então aparecem como chips que se removem.
  const atrasadasAplicado = url.texto('atrasadas') === 'true'
  const centroAplicado = url.texto('centro').trim()
  const fimDeAplicado = ehDataIso(url.texto('fimDe')) ? url.texto('fimDe') : ''
  const fimAteAplicado = ehDataIso(url.texto('fimAte')) ? url.texto('fimAte') : ''
  const periodoValido = !periodoInvertido(deAplicado, ateAplicado) && !periodoInvertido(fimDeAplicado, fimAteAplicado)
  const pagina = url.numero('pagina', 0)
  const tamanho = url.numero('tamanho', 25)

  // Os campos editam um rascunho; a consulta só muda ao clicar em Pesquisar.
  const [busca, setBusca] = useState(buscaAplicada)
  const [status, setStatus] = useState<StatusOrdem | 'TODAS'>(statusAplicado ?? 'TODAS')
  const [de, setDe] = useState(deAplicado)
  const [ate, setAte] = useState(ateAplicado)
  useEffect(() => {
    setBusca(buscaAplicada); setStatus(statusAplicado ?? 'TODAS'); setDe(deAplicado); setAte(ateAplicado)
  }, [buscaAplicada, statusAplicado, deAplicado, ateAplicado])

  const [ordemSelecionada, setOrdemSelecionada] = useState<OrdemProducao | null>(null)

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['ordens', 'pesquisa', buscaAplicada, statusAplicado, deAplicado, ateAplicado, atrasadasAplicado, centroAplicado, fimDeAplicado, fimAteAplicado, pagina, tamanho],
    queryFn: () => pesquisarOrdens({
      busca: buscaAplicada || undefined, status: statusAplicado, de: deAplicado || undefined, ate: ateAplicado || undefined,
      atrasadas: atrasadasAplicado || undefined, centro: centroAplicado || undefined,
      fimDe: fimDeAplicado || undefined, fimAte: fimAteAplicado || undefined, pagina, tamanho,
    }),
    enabled: periodoValido,
    placeholderData: keepPreviousData,
  })
  const ordens = data?.itens ?? []
  const total = data?.total ?? 0
  const tabela = useTabelaInterativa(ordens, colunasDeOrdens)
  const filtrando = buscaAplicada !== '' || statusAplicado !== undefined || deAplicado !== '' || ateAplicado !== '' || atrasadasAplicado
    || centroAplicado !== '' || fimDeAplicado !== '' || fimAteAplicado !== ''

  // Se a página pedida deixou de existir (ex.: uma ordem saiu do filtro), volta para a última.
  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      url.atualizar({ pagina: Math.max(0, data.totalPaginas - 1) })
    }
  }, [data, pagina]) // eslint-disable-line react-hooks/exhaustive-deps

  const pesquisar = () => {
    if (periodoInvertido(de, ate)) return
    url.atualizar({ busca: busca.trim(), status: status === 'TODAS' ? undefined : status, de, ate, pagina: undefined })
  }
  const limpar = () => {
    setBusca(''); setStatus('TODAS'); setDe(''); setAte('')
    url.atualizar({ busca: undefined, status: undefined, de: undefined, ate: undefined, atrasadas: undefined, centro: undefined, fimDe: undefined, fimAte: undefined, pagina: undefined })
  }

  return (
    <Box>
      <PageHeader
        titulo="Ordens de Produção"
        subtitulo={isLoading ? '…' : `${total} ${filtrando ? 'ordens encontradas' : 'ordens cadastradas'}`}
        breadcrumbs={[{ label: 'Produção' }, { label: 'Ordens', href: '/ordens' }, { label: 'Resultados' }]}
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroOrdens busca={busca} onBusca={setBusca} status={status} onStatus={setStatus} de={de} onDe={setDe} ate={ate} onAte={setAte} />
      </PainelDeConsulta>

      {(atrasadasAplicado || centroAplicado !== '' || fimDeAplicado !== '' || fimAteAplicado !== '') && (
        <Box sx={{ mb: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          {atrasadasAplicado && (
            <Chip label="Somente ordens atrasadas" color="error" variant="outlined" onDelete={() => url.atualizar({ atrasadas: undefined, pagina: undefined })} />
          )}
          {centroAplicado !== '' && (
            <Chip label={`Centro: ${centroAplicado}`} variant="outlined" onDelete={() => url.atualizar({ centro: undefined, pagina: undefined })} />
          )}
          {(fimDeAplicado !== '' || fimAteAplicado !== '') && (
            <Chip label={rotuloDoFim(fimDeAplicado, fimAteAplicado)} variant="outlined" onDelete={() => url.atualizar({ fimDe: undefined, fimAte: undefined, pagina: undefined })} />
          )}
        </Box>
      )}

      {!periodoValido && <Alert severity="warning" sx={{ mb: 2 }}>O fim do período não pode ser anterior ao início.</Alert>}
      {isError && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(error, 'Não foi possível carregar as ordens.')}</Alert>}

      <Card>
        {isLoading && periodoValido ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : ordens.length === 0 ? (
          <EmptyState
            Icone={ListAltIcon}
            titulo={filtrando ? 'Nenhuma ordem encontrada com os filtros' : 'Nenhuma ordem cadastrada'}
            descricao={filtrando ? 'Tente ajustar os filtros de busca.' : undefined}
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={ordens.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
            <OrdemTable ordens={tabela.linhas} onAtualizarStatus={setOrdemSelecionada} controle={tabela.controle} />
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

      <AtualizarStatusDialog ordem={ordemSelecionada} onFechar={() => setOrdemSelecionada(null)} />
    </Box>
  )
}
