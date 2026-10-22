import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Skeleton from '@mui/material/Skeleton'
import WarehouseIcon from '@mui/icons-material/WarehouseOutlined'
import { useQuery } from '@tanstack/react-query'
import { pesquisarEstoque } from '../api/estoque'
import { AlertaDeEstoque } from '../components/suprimentos/AlertaDeEstoque'
import { CamposFiltroEstoque } from '../components/suprimentos/CamposFiltroEstoque'
import { EstoqueTable } from '../components/suprimentos/EstoqueTable'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { CartaoDeRecentes } from '../components/consulta/CartaoDeRecentes'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { montarBusca } from '../utils/url'
import type { TipoMaterial } from '../types'

/** Estoque, primeira etapa: escolher o que consultar (ou abrir um dos 10 materiais com entrada mais recente). */
export function EstoquePage() {
  const navigate = useNavigate()
  const [busca, setBusca] = useState('')
  const [tipo, setTipo] = useState<TipoMaterial | 'TODOS'>('TODOS')
  const [somenteComSaldo, setSomenteComSaldo] = useState(false)

  const recentes = useQuery({
    queryKey: ['estoque', 'recentes', 10],
    queryFn: () => pesquisarEstoque({ pagina: 0, tamanho: 10 }),
  })

  const verResultados = (filtros: Record<string, string | undefined> = {}) =>
    navigate({ pathname: '/estoque/resultados', search: montarBusca(filtros) })
  const pesquisar = () => verResultados({
    busca: busca.trim(),
    tipo: tipo === 'TODOS' ? undefined : tipo,
    somenteComSaldo: somenteComSaldo ? 'true' : undefined,
  })
  const limpar = () => { setBusca(''); setTipo('TODOS'); setSomenteComSaldo(false) }

  return (
    <Box>
      <PageHeader
        titulo="Estoque"
        subtitulo="Pesquise por código, descrição ou tipo, ou veja as últimas entradas · saldo calculado a partir dos lotes"
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Estoque' }]}
      />

      <AlertaDeEstoque />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroEstoque busca={busca} onBusca={setBusca} tipo={tipo} onTipo={setTipo}
          somenteComSaldo={somenteComSaldo} onSomenteComSaldo={setSomenteComSaldo} />
      </PainelDeConsulta>

      <CartaoDeRecentes titulo="10 materiais com entrada mais recente" onVerTodas={() => verResultados()}>
        {recentes.isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : recentes.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>{mensagemDeErroAoCarregar(recentes.error, 'Não foi possível carregar o estoque.')}</Alert>
        ) : (recentes.data?.itens.length ?? 0) === 0 ? (
          <EmptyState
            Icone={WarehouseIcon}
            titulo="Nenhum material cadastrado"
            descricao="O estoque nasce das entradas de notas fiscais e das ordens de produção concluídas."
          />
        ) : (
          <EstoqueTable posicoes={recentes.data!.itens} />
        )}
      </CartaoDeRecentes>
    </Box>
  )
}
