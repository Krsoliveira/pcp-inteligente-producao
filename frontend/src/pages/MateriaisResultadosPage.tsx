import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Skeleton from '@mui/material/Skeleton'
import InventoryIcon from '@mui/icons-material/Inventory2Outlined'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pesquisarMateriais } from '../api/materiais'
import { MaterialTable, colunasDeMateriais } from '../components/materiais/MaterialTable'
import { CamposFiltroMateriais, TIPOS_VALIDOS } from '../components/materiais/CamposFiltroMateriais'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { EmptyState } from '../components/EmptyState'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { type TipoMaterial } from '../types'

/** Segunda etapa: o resultado da consulta de materiais, paginado no servidor, com filtro e ordenação por coluna na página. */
export function MateriaisResultadosPage() {
  const url = useParametrosDaUrl()
  const buscaAplicada = url.texto('busca')
  const tipoBruto = url.texto('tipo') as TipoMaterial
  const tipoAplicado = TIPOS_VALIDOS.includes(tipoBruto) ? tipoBruto : undefined
  const pagina = url.numero('pagina', 0)
  const tamanho = url.numero('tamanho', 25)

  const [busca, setBusca] = useState(buscaAplicada)
  const [tipo, setTipo] = useState<TipoMaterial | 'TODOS'>(tipoAplicado ?? 'TODOS')
  useEffect(() => { setBusca(buscaAplicada); setTipo(tipoAplicado ?? 'TODOS') }, [buscaAplicada, tipoAplicado])

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['materiais', 'pesquisa', buscaAplicada, tipoAplicado, pagina, tamanho],
    queryFn: () => pesquisarMateriais({ busca: buscaAplicada || undefined, tipo: tipoAplicado, pagina, tamanho }),
    placeholderData: keepPreviousData,
  })
  const materiais = data?.itens ?? []
  const total = data?.total ?? 0
  const tabela = useTabelaInterativa(materiais, colunasDeMateriais)
  const filtrando = buscaAplicada !== '' || tipoAplicado !== undefined

  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      url.atualizar({ pagina: Math.max(0, data.totalPaginas - 1) })
    }
  }, [data, pagina]) // eslint-disable-line react-hooks/exhaustive-deps

  const pesquisar = () => url.atualizar({ busca: busca.trim(), tipo: tipo === 'TODOS' ? undefined : tipo, pagina: undefined })
  const limpar = () => { setBusca(''); setTipo('TODOS'); url.atualizar({ busca: undefined, tipo: undefined, pagina: undefined }) }

  return (
    <Box>
      <PageHeader
        titulo="Materiais"
        subtitulo={isLoading ? '…' : `${total} ${filtrando ? 'materiais encontrados' : 'materiais cadastrados'}`}
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Materiais', href: '/materiais' }, { label: 'Resultados' }]}
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroMateriais busca={busca} onBusca={setBusca} tipo={tipo} onTipo={setTipo} />
      </PainelDeConsulta>

      {isError && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(error, 'Não foi possível carregar os materiais.')}</Alert>}

      <Card>
        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : materiais.length === 0 ? (
          <EmptyState
            Icone={InventoryIcon}
            titulo={filtrando ? 'Nenhum material encontrado com os filtros' : 'Nenhum material cadastrado'}
            descricao={filtrando ? 'Tente ajustar os filtros de busca.' : undefined}
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={materiais.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
            <MaterialTable materiais={tabela.linhas} controle={tabela.controle} />
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
