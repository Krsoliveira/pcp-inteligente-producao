import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Skeleton from '@mui/material/Skeleton'
import WarehouseIcon from '@mui/icons-material/WarehouseOutlined'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pesquisarEstoque } from '../api/estoque'
import { AlertaDeEstoque } from '../components/suprimentos/AlertaDeEstoque'
import { CamposFiltroEstoque, TIPOS_DE_ESTOQUE_VALIDOS } from '../components/suprimentos/CamposFiltroEstoque'
import { EstoqueTable, colunasDeEstoque } from '../components/suprimentos/EstoqueTable'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { EmptyState } from '../components/EmptyState'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { type TipoMaterial } from '../types'

/** Estoque, segunda etapa: o resultado da consulta, paginado no servidor, com filtro e ordenação por coluna na página. */
export function EstoqueResultadosPage() {
  const url = useParametrosDaUrl()
  const buscaAplicada = url.texto('busca')
  const tipoBruto = url.texto('tipo') as TipoMaterial
  const tipoAplicado = TIPOS_DE_ESTOQUE_VALIDOS.includes(tipoBruto) ? tipoBruto : undefined
  const saldoAplicado = url.texto('somenteComSaldo') === 'true'
  const pagina = url.numero('pagina', 0)
  const tamanho = url.numero('tamanho', 25)

  const [busca, setBusca] = useState(buscaAplicada)
  const [tipo, setTipo] = useState<TipoMaterial | 'TODOS'>(tipoAplicado ?? 'TODOS')
  const [somenteComSaldo, setSomenteComSaldo] = useState(saldoAplicado)
  useEffect(() => {
    setBusca(buscaAplicada); setTipo(tipoAplicado ?? 'TODOS'); setSomenteComSaldo(saldoAplicado)
  }, [buscaAplicada, tipoAplicado, saldoAplicado])

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['estoque', 'pesquisa', buscaAplicada, tipoAplicado, saldoAplicado, pagina, tamanho],
    queryFn: () => pesquisarEstoque({
      busca: buscaAplicada || undefined, tipo: tipoAplicado, somenteComSaldo: saldoAplicado || undefined, pagina, tamanho,
    }),
    placeholderData: keepPreviousData,
  })
  const posicoes = data?.itens ?? []
  const total = data?.total ?? 0
  const tabela = useTabelaInterativa(posicoes, colunasDeEstoque)
  const filtrando = buscaAplicada !== '' || tipoAplicado !== undefined || saldoAplicado

  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      url.atualizar({ pagina: Math.max(0, data.totalPaginas - 1) })
    }
  }, [data, pagina]) // eslint-disable-line react-hooks/exhaustive-deps

  const pesquisar = () => url.atualizar({
    busca: busca.trim(),
    tipo: tipo === 'TODOS' ? undefined : tipo,
    somenteComSaldo: somenteComSaldo ? 'true' : undefined,
    pagina: undefined,
  })
  const limpar = () => {
    setBusca(''); setTipo('TODOS'); setSomenteComSaldo(false)
    url.atualizar({ busca: undefined, tipo: undefined, somenteComSaldo: undefined, pagina: undefined })
  }

  return (
    <Box>
      <PageHeader
        titulo="Estoque"
        subtitulo={isLoading ? '…' : `${total} ${filtrando ? 'materiais encontrados' : 'materiais'} · saldo calculado a partir dos lotes`}
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Estoque', href: '/estoque' }, { label: 'Resultados' }]}
      />

      <AlertaDeEstoque />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroEstoque busca={busca} onBusca={setBusca} tipo={tipo} onTipo={setTipo}
          somenteComSaldo={somenteComSaldo} onSomenteComSaldo={setSomenteComSaldo} />
      </PainelDeConsulta>

      {isError && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(error, 'Não foi possível carregar o estoque.')}</Alert>}

      <Card>
        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : posicoes.length === 0 ? (
          <EmptyState
            Icone={WarehouseIcon}
            titulo={filtrando ? 'Nenhum material encontrado com os filtros' : 'Nenhum material cadastrado'}
            descricao={filtrando ? 'Tente ajustar os filtros de busca.' : 'O estoque nasce das entradas de notas fiscais e das ordens de produção concluídas.'}
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={posicoes.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
            <EstoqueTable posicoes={tabela.linhas} controle={tabela.controle} />
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
