import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import Card from '@mui/material/Card'
import Chip from '@mui/material/Chip'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import Skeleton from '@mui/material/Skeleton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { compararListasTecnicas, listarListasTecnicas } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { StatusListaBadge } from '../components/StatusBadge'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { formatarCodigoMaterial, formatarQuantidade } from '../utils/formatacao'
import { mensagemDeErroDaApi } from '../utils/mensagensDeErro'
import type { ComparacaoDeVersoes, ItemComparado } from '../types'

type Situacao = 'alterado' | 'adicionado' | 'removido' | 'igual'

const SITUACAO: Record<Situacao, { rotulo: string; cor: string }> = {
  alterado: { rotulo: 'Alterado', cor: '#B45309' },
  adicionado: { rotulo: 'Adicionado em B', cor: '#047857' },
  removido: { rotulo: 'Removido em B', cor: '#B91C1C' },
  igual: { rotulo: 'Igual', cor: '#64748B' },
}

const contagem = (n: number, singular: string, plural: string) => `${n} ${n === 1 ? singular : plural}`

/** Linhas da comparação, das diferenças para o que não mudou. */
function linhasDa(comparacao: ComparacaoDeVersoes): Array<{ situacao: Situacao; item: ItemComparado }> {
  return [
    ...comparacao.alterados.map((item) => ({ situacao: 'alterado' as const, item })),
    ...comparacao.adicionados.map((item) => ({ situacao: 'adicionado' as const, item })),
    ...comparacao.removidos.map((item) => ({ situacao: 'removido' as const, item })),
    ...comparacao.iguais.map((item) => ({ situacao: 'igual' as const, item })),
  ]
}

/**
 * Comparação de duas versões da mesma lista técnica (ADR-0019): o que foi adicionado, removido ou
 * alterado de A para B. As versões vêm na URL (?a=&b=) e podem ser trocadas na própria tela.
 */
export function ListaTecnicaComparacaoPage() {
  const url = useParametrosDaUrl()
  const a = url.texto('a')
  const b = url.texto('b')

  const comparacaoQuery = useQuery({
    queryKey: ['listas-tecnicas', 'comparacao', a, b],
    queryFn: () => compararListasTecnicas(a, b),
    enabled: !!a && !!b,
    retry: false,
    placeholderData: keepPreviousData,
  })
  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))
  const comparacao = comparacaoQuery.data

  // O produto da última comparação que deu certo: mantém os seletores na tela mesmo se a próxima falhar.
  const [materialId, setMaterialId] = useState<string>()
  if (comparacao && comparacao.versaoA.materialId !== materialId) setMaterialId(comparacao.versaoA.materialId)
  const { data: versoes = [] } = useQuery({
    queryKey: ['listas-tecnicas', 'versoes', materialId],
    queryFn: () => listarListasTecnicas(materialId!),
    enabled: !!materialId,
  })

  const material = materialId ? materialPorId.get(materialId) : undefined
  const codigo = material ? formatarCodigoMaterial(material.codigo) : '—'

  const seletor = (rotulo: string, valor: string, onChange: (id: string) => void) => (
    <TextField select size="small" label={rotulo} value={versoes.some((v) => v.id === valor) ? valor : ''}
      onChange={(e) => onChange(e.target.value)} sx={{ minWidth: 200 }}>
      {versoes.map((v) => <MenuItem key={v.id} value={v.id}>{v.versao}</MenuItem>)}
    </TextField>
  )

  // Escolher de um lado a versão que está do outro inverte a comparação (v2 → v1 vira v1 → v2).
  const escolherA = (id: string) => url.atualizar(id === b ? { a: id, b: a } : { a: id })
  const escolherB = (id: string) => url.atualizar(id === a ? { a: b, b: id } : { b: id })

  return (
    <Box>
      <PageHeader
        titulo="Comparar versões"
        subtitulo={material ? `${codigo} — ${material.descricao}` : 'Escolha duas versões da mesma lista técnica'}
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Listas Técnicas', href: '/listas-tecnicas' }, { label: 'Comparar' }]}
      />

      {(!a || !b) && <Alert severity="info">Abra uma lista técnica e use "Comparar" em uma das outras versões.</Alert>}

      {materialId && (
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center', mb: 2 }}>
          {seletor('Versão A', a, escolherA)}
          <Tooltip title="Inverter versões">
            <IconButton aria-label="Inverter versões" size="small" onClick={() => url.atualizar({ a: b, b: a })}>
              <SwapHorizIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {seletor('Versão B', b, escolherB)}
          {comparacao && !comparacaoQuery.isError && (
            <>
              <StatusListaBadge status={comparacao.versaoA.status} />
              <StatusListaBadge status={comparacao.versaoB.status} />
            </>
          )}
        </Box>
      )}

      {comparacaoQuery.isLoading && <Skeleton variant="rounded" height={240} />}
      {comparacaoQuery.isError && (
        <Alert severity="error">{mensagemDeErroDaApi(comparacaoQuery.error, 'Não foi possível comparar as versões.')}</Alert>
      )}

      {comparacao && !comparacaoQuery.isError && (
        <>

          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
            <Chip size="small" label={contagem(comparacao.alterados.length, 'alterado', 'alterados')} />
            <Chip size="small" label={contagem(comparacao.adicionados.length, 'adicionado', 'adicionados')} />
            <Chip size="small" label={contagem(comparacao.removidos.length, 'removido', 'removidos')} />
            <Chip size="small" label={contagem(comparacao.iguais.length, 'igual', 'iguais')} />
          </Box>

          {comparacao.versaoA.quantidadeBase !== comparacao.versaoB.quantidadeBase && (
            <Alert severity="info" sx={{ mb: 2 }}>
              As versões usam bases diferentes: a {comparacao.versaoA.versao} vale para {comparacao.versaoA.quantidadeBase.toLocaleString('pt-BR')} e a{' '}
              {comparacao.versaoB.versao} para {comparacao.versaoB.quantidadeBase.toLocaleString('pt-BR')}. A comparação é por unidade produzida; as
              quantidades abaixo são as de cada versão.
            </Alert>
          )}

          {!comparacao.temDiferencas && (
            <Alert severity="success" sx={{ mb: 2 }}>
              As versões {comparacao.versaoA.versao} e {comparacao.versaoB.versao} têm os mesmos componentes e quantidades.
            </Alert>
          )}

          <Card>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Situação</TableCell>
                    <TableCell>Código</TableCell>
                    <TableCell>Descrição</TableCell>
                    <TableCell align="right">Versão {comparacao.versaoA.versao}</TableCell>
                    <TableCell align="right">Versão {comparacao.versaoB.versao}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {linhasDa(comparacao).map(({ situacao, item }) => {
                    const componente = materialPorId.get(item.materialComponenteId)
                    const { rotulo, cor } = SITUACAO[situacao]
                    return (
                      <TableRow key={`${situacao}-${item.materialComponenteId}`} hover>
                        <TableCell>
                          <Chip label={rotulo} size="small" sx={{ bgcolor: `${cor}14`, color: cor, fontWeight: 600, fontSize: '0.7rem', border: 'none' }} />
                        </TableCell>
                        <TableCell>
                          <Typography
                            variant="body2"
                            noWrap
                            sx={{ fontWeight: 600, fontFamily: 'monospace' }}>
                            {componente ? formatarCodigoMaterial(componente.codigo) : '—'}
                          </Typography>
                        </TableCell>
                        <TableCell>{componente?.descricao ?? `${item.materialComponenteId.substring(0, 8)}…`}</TableCell>
                        <TableCell align="right">{item.quantidadeA === null ? '—' : formatarQuantidade(item.quantidadeA, item.unidadeA ?? undefined)}</TableCell>
                        <TableCell align="right">{item.quantidadeB === null ? '—' : formatarQuantidade(item.quantidadeB, item.unidadeB ?? undefined)}</TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
        </>
      )}
    </Box>
  )
}
