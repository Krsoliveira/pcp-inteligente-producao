import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Snackbar from '@mui/material/Snackbar'
import AddIcon from '@mui/icons-material/Add'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongOutlined'
import { useQuery } from '@tanstack/react-query'
import { pesquisarNotasFiscais } from '../api/notasFiscais'
import { listarMateriais } from '../api/materiais'
import { EntradaNotaFiscalDialog } from '../components/suprimentos/EntradaNotaFiscalDialog'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { NotaFiscalTable, colunasDeNotas } from '../components/suprimentos/NotaFiscalTable'
import { CamposFiltroNotas } from '../components/suprimentos/CamposFiltroNotas'
import { periodoInvertido } from '../components/consulta/CamposDePeriodo'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { CartaoDeRecentes } from '../components/consulta/CartaoDeRecentes'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { montarBusca } from '../utils/url'
import type { NotaFiscal } from '../types'

/**
 * Entrada de notas (ADR-0012), primeira etapa: escolher o que consultar (ou abrir uma das 10
 * notas recebidas mais recentemente) e lançar novas. Cada item lançado vira um lote no estoque.
 */
export function NotasFiscaisPage() {
  const navigate = useNavigate()
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [dialogAberto, setDialogAberto] = useState(false)
  const [registrada, setRegistrada] = useState<NotaFiscal | null>(null)

  const recentes = useQuery({
    queryKey: ['notas-fiscais', 'recentes', 10],
    queryFn: () => pesquisarNotasFiscais({ pagina: 0, tamanho: 10 }),
  })
  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))

  const itensRecentes = recentes.data?.itens ?? []
  const tabela = useTabelaInterativa(itensRecentes, colunasDeNotas(materialPorId))

  const verResultados = (filtros: Record<string, string | undefined> = {}) =>
    navigate({ pathname: '/notas-fiscais/resultados', search: montarBusca(filtros) })
  const pesquisar = () => {
    if (periodoInvertido(de, ate)) return
    verResultados({ de, ate })
  }
  const limpar = () => { setDe(''); setAte('') }

  return (
    <Box>
      <PageHeader
        titulo="Entrada de notas"
        subtitulo="Escolha o período de recebimento, ou abra uma das mais recentes"
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Entrada de notas' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
            Nova entrada
          </Button>
        }
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroNotas de={de} onDe={setDe} ate={ate} onAte={setAte} />
      </PainelDeConsulta>

      <CartaoDeRecentes titulo="10 notas recebidas mais recentemente" onVerTodas={() => verResultados()}>
        {recentes.isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : recentes.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>{mensagemDeErroAoCarregar(recentes.error, 'Não foi possível carregar as notas fiscais.')}</Alert>
        ) : (recentes.data?.itens.length ?? 0) === 0 ? (
          <EmptyState
            Icone={ReceiptLongIcon}
            titulo="Nenhuma nota fiscal registrada"
            descricao="Lance a primeira nota de compra: cada item vira um lote de matéria-prima no estoque."
            acaoLabel="Nova entrada"
            onAcao={() => setDialogAberto(true)}
          />
        ) : (
          <>
          <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={itensRecentes.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
          <NotaFiscalTable notas={tabela.linhas} materialPorId={materialPorId} controle={tabela.controle} />
          </>
        )}
      </CartaoDeRecentes>

      <EntradaNotaFiscalDialog aberto={dialogAberto} onFechar={() => setDialogAberto(false)} onRegistrada={setRegistrada} />
      <Snackbar
        open={!!registrada}
        autoHideDuration={6000}
        onClose={() => setRegistrada(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled" onClose={() => setRegistrada(null)}
          action={registrada && (
            <Button color="inherit" size="small" onClick={() => navigate(`/notas-fiscais/${registrada.id}`)}>Ver nota</Button>
          )}>
          Nota {registrada?.numero} registrada: {registrada?.quantidadeItens}{' '}
          {registrada?.quantidadeItens === 1 ? 'lote entrou' : 'lotes entraram'} no estoque.
        </Alert>
      </Snackbar>
    </Box>
  )
}
