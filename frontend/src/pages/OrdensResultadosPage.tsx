import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Skeleton from '@mui/material/Skeleton'
import ListAltIcon from '@mui/icons-material/ListAlt'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pesquisarOrdens } from '../api/ordens'
import { OrdemTable } from '../components/OrdemTable'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { EmptyState } from '../components/EmptyState'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { SubfiltroTabela, useSubfiltro } from '../components/consulta/SubfiltroTabela'
import { CamposFiltroOrdens, STATUS_VALIDOS } from '../components/ordens/CamposFiltroOrdens'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import type { OrdemProducao, StatusOrdem } from '../types'

/** Segunda etapa: o resultado da consulta, paginado no servidor, com subfiltro na página. */
export function OrdensResultadosPage() {
  const url = useParametrosDaUrl()
  const buscaAplicada = url.texto('busca')
  const statusBruto = url.texto('status') as StatusOrdem
  const statusAplicado = STATUS_VALIDOS.includes(statusBruto) ? statusBruto : undefined
  const pagina = url.numero('pagina', 0)
  const tamanho = url.numero('tamanho', 25)

  // Os campos editam um rascunho; a consulta só muda ao clicar em Pesquisar.
  const [busca, setBusca] = useState(buscaAplicada)
  const [status, setStatus] = useState<StatusOrdem | 'TODAS'>(statusAplicado ?? 'TODAS')
  useEffect(() => { setBusca(buscaAplicada); setStatus(statusAplicado ?? 'TODAS') }, [buscaAplicada, statusAplicado])

  const [ordemSelecionada, setOrdemSelecionada] = useState<OrdemProducao | null>(null)

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['ordens', 'pesquisa', buscaAplicada, statusAplicado, pagina, tamanho],
    queryFn: () => pesquisarOrdens({ busca: buscaAplicada || undefined, status: statusAplicado, pagina, tamanho }),
    placeholderData: keepPreviousData,
  })
  const ordens = data?.itens ?? []
  const total = data?.total ?? 0
  const subfiltro = useSubfiltro(ordens, (o) => `${o.codigo} ${o.centroDeTrabalho}`)
  const filtrando = buscaAplicada !== '' || statusAplicado !== undefined

  // Se a página pedida deixou de existir (ex.: uma ordem saiu do filtro), volta para a última.
  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      url.atualizar({ pagina: Math.max(0, data.totalPaginas - 1) })
    }
  }, [data, pagina]) // eslint-disable-line react-hooks/exhaustive-deps

  const pesquisar = () => url.atualizar({ busca: busca.trim(), status: status === 'TODAS' ? undefined : status, pagina: undefined })
  const limpar = () => { setBusca(''); setStatus('TODAS'); url.atualizar({ busca: undefined, status: undefined, pagina: undefined }) }

  return (
    <Box>
      <PageHeader
        titulo="Ordens de Produção"
        subtitulo={isLoading ? '…' : `${total} ${filtrando ? 'ordens encontradas' : 'ordens cadastradas'}`}
        breadcrumbs={[{ label: 'Produção' }, { label: 'Ordens', href: '/ordens' }, { label: 'Resultados' }]}
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroOrdens busca={busca} onBusca={setBusca} status={status} onStatus={setStatus} />
      </PainelDeConsulta>

      {isError && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(error, 'Não foi possível carregar as ordens.')}</Alert>}

      <Card>
        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : ordens.length === 0 ? (
          <EmptyState
            Icone={ListAltIcon}
            titulo={filtrando ? 'Nenhuma ordem encontrada com os filtros' : 'Nenhuma ordem cadastrada'}
            descricao={filtrando ? 'Tente ajustar os filtros de busca.' : undefined}
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <SubfiltroTabela valor={subfiltro.texto} onChange={subfiltro.setTexto} exibidas={subfiltro.filtrados.length} naPagina={ordens.length} />
            <OrdemTable ordens={subfiltro.filtrados} onAtualizarStatus={setOrdemSelecionada} />
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
