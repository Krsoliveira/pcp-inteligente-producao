import { useState } from 'react'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
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
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/AddCircleOutline'
import DeleteIcon from '@mui/icons-material/DeleteOutline'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { lancarConsumo, listarConsumosPorOrdem } from '../api/consumos'
import { listarMateriais } from '../api/materiais'
import { buscarOrdemPorId } from '../api/ordens'
import { listarTiposOrdem } from '../api/tiposOrdem'
import { AlocacaoDeLotes } from '../components/consumo/AlocacaoDeLotes'
import { lerQuantidade } from '../components/consumo/alocacao'
import { RASCUNHO_VAZIO, lerQuantidadeOpcional, montarLancamento, type ItemAdicionado, type Rascunho } from '../components/consumo/lancamento'
import { PageHeader } from '../components/PageHeader'
import { usePermissoes } from '../hooks/usePermissoes'
import { formatarCodigoMaterial, formatarQuantidade } from '../utils/formatacao'
import { mensagemDeErroAoCarregar, mensagemDeErroDaApi } from '../utils/mensagensDeErro'
import type { ConsumoMaterial, Material } from '../types'

let contadorDeItens = 0

/** O texto de ajuda fica sob o campo sem ocupar espaço: aparecer ou sumir não desloca o campo na linha. */
const AJUDA_SEM_DESLOCAR = { formHelperText: { sx: { position: 'absolute', top: '100%', mx: 0, mt: 0.25, whiteSpace: 'nowrap' } } } as const
/** Espaço fixo sob os campos da linha para o texto de ajuda, igual em todas as células para mantê-las alinhadas. */
const ESPACO_PARA_AJUDA = { '& > td': { pb: 3.5 } } as const

/**
 * Lançamento do consumo de uma ordem em produção, com todos os componentes numa tela (ADR-0026). O que ficou em
 * branco não é lançado; o que foi preenchido, e os itens adicionados fora da lista, entram juntos: tudo ou nada.
 */
export function LancamentoDeConsumoPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { tem } = usePermissoes()
  const [rascunhos, setRascunhos] = useState<Record<string, Rascunho>>({})
  const [adicionados, setAdicionados] = useState<ItemAdicionado[]>([])
  const [erro, setErro] = useState<string | null>(null)

  const ordemQuery = useQuery({ queryKey: ['ordem', id], queryFn: () => buscarOrdemPorId(id!), enabled: !!id })
  const consumosQuery = useQuery({ queryKey: ['consumos', id], queryFn: () => listarConsumosPorOrdem(id!), enabled: !!id && tem('CONSUMO_VISUALIZAR') })
  const podeVerMateriais = tem('MATERIAL_VISUALIZAR')
  const materiaisQuery = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais, enabled: podeVerMateriais })
  const podeAdicionar = tem('CONSUMO_ADICIONAR_ITEM') && podeVerMateriais
  const tiposQuery = useQuery({ queryKey: ['tipos-ordem'], queryFn: listarTiposOrdem, enabled: podeAdicionar && tem('TIPO_ORDEM_VISUALIZAR') })

  const ordem = ordemQuery.data
  const consumos = consumosQuery.data ?? []
  const materiais = materiaisQuery.data ?? []
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))
  const rotulo = (materialId: string) => {
    const m = materialPorId.get(materialId)
    return m ? formatarCodigoMaterial(m.codigo) : 'Material'
  }
  const pendentes = consumos.filter((c) => !c.registrado)
  const lancados = consumos.filter((c) => c.registrado)

  // Produto acabado só entra em ordem do tipo Conversão (ADR-0024); o servidor é quem decide.
  const ordemDeConversao = tiposQuery.data?.find((t) => t.id === ordem?.tipoOrdemId)?.conversao === true
  const jaNaOrdem = new Set([...consumos.map((c) => c.materialId), ...adicionados.map((a) => a.materialId)])
  const opcoesDeItem = (atual: string): Material[] => materiais.filter((m) =>
    m.id === atual || (
      !jaNaOrdem.has(m.id) && m.id !== ordem?.materialId && (m.tipo !== 'PRODUTO_ACABADO' || ordemDeConversao)
    ))

  // Editar qualquer campo limpa o aviso anterior: ele se refere ao que foi enviado, não ao que está na tela agora.
  const alterar = (chave: string, mudanca: Partial<Rascunho>) => {
    setErro(null)
    setRascunhos((atuais) => ({ ...atuais, [chave]: { ...(atuais[chave] ?? RASCUNHO_VAZIO), ...mudanca } }))
  }
  const alterarItem = (chave: string, mudanca: Partial<ItemAdicionado>) => {
    setErro(null)
    setAdicionados((atuais) => atuais.map((i) => (i.chave === chave ? { ...i, ...mudanca } : i)))
  }
  const adicionarItem = () =>
    setAdicionados((atuais) => [...atuais, { ...RASCUNHO_VAZIO, chave: `novo-${++contadorDeItens}`, materialId: '' }])

  const { mutate, isPending } = useMutation({
    mutationFn: () => {
      const resultado = montarLancamento(pendentes, rascunhos, adicionados, rotulo)
      if ('erro' in resultado) return Promise.reject(new Error(resultado.erro))
      return lancarConsumo(id!, resultado.corpo)
    },
    onSuccess: () => {
      for (const chave of [['ordem', id], ['consumos', id], ['lotes-ordem', id], ['ordens'], ['lotes'], ['estoque'], ['lotes-disponiveis'], ['auditoria']]) {
        queryClient.invalidateQueries({ queryKey: chave })
      }
      navigate(`/ordens/${id}`)
    },
    onError: (e: unknown) => setErro(e instanceof Error && !('response' in e) ? e.message : mensagemDeErroDaApi(e, 'Não foi possível lançar o consumo. Nada foi lançado.')),
  })

  const lancar = () => {
    setErro(null)
    const resultado = montarLancamento(pendentes, rascunhos, adicionados, rotulo)
    if ('erro' in resultado) { setErro(resultado.erro); return }
    mutate()
  }

  if (ordemQuery.isLoading || consumosQuery.isLoading) return <Skeleton variant="rounded" height={320} />
  if (ordemQuery.isError || !ordem) return <Alert severity="error">{mensagemDeErroAoCarregar(ordemQuery.error, 'Ordem não encontrada.')}</Alert>
  const voltar = `/ordens/${ordem.id}`

  return (
    <Box>
      <PageHeader
        titulo="Lançar consumo"
        subtitulo={`Ordem ${ordem.codigo}`}
        breadcrumbs={[{ label: 'Produção' }, { label: 'Ordens', href: '/ordens' }, { label: ordem.codigo, href: voltar }, { label: 'Lançar consumo' }]}
      />

      {ordem.status !== 'EM_PRODUCAO' ? (
        <Alert severity="info" action={<Button component={RouterLink} to={voltar} size="small">Voltar à ordem</Button>}>
          O consumo só pode ser lançado em ordens em produção.
        </Alert>
      ) : (
        <>
          <Alert severity="info" sx={{ mb: 2 }}>
            Informe o consumo dos componentes. O que ficar em branco não é lançado agora. O lançamento é feito de uma vez: se
            qualquer linha for recusada, nada é lançado.
          </Alert>
          {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}

          <Card sx={{ mb: 2 }}>
            <CardContent sx={{ p: 0 }}>
              <TableContainer>
                <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Componente</TableCell>
                      <TableCell align="right">Planejado</TableCell>
                      <TableCell sx={{ width: 150 }}>Consumido</TableCell>
                      <TableCell sx={{ minWidth: 220 }}>Justificativa do desvio</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pendentes.length === 0 && adicionados.length === 0 && (
                      <TableRow><TableCell colSpan={4}><Typography variant="body2" color="text.secondary">Todas as linhas da lista já foram lançadas.</Typography></TableCell></TableRow>
                    )}
                    {pendentes.map((c) => (
                      <LinhaDaLista key={c.id} consumo={c} material={materialPorId.get(c.materialId)}
                        rascunho={rascunhos[c.id] ?? RASCUNHO_VAZIO} onChange={(m) => alterar(c.id, m)} />
                    ))}
                    {adicionados.map((item) => (
                      <LinhaAdicionada key={item.chave} item={item} opcoes={opcoesDeItem(item.materialId)} materialPorId={materialPorId}
                        onChange={(m) => alterarItem(item.chave, m)}
                        onRemover={() => setAdicionados((atuais) => atuais.filter((i) => i.chave !== item.chave))} />
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>

          {podeAdicionar && (
            <Button startIcon={<AddIcon />} onClick={adicionarItem} sx={{ mb: 2 }}>Adicionar item fora da lista</Button>
          )}

          {lancados.length > 0 && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
              Já lançados ({lancados.length}): {lancados.map((c) => rotulo(c.materialId)).join(', ')}.
            </Typography>
          )}

          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            <Button component={RouterLink} to={voltar} disabled={isPending}>Cancelar</Button>
            <Button variant="contained" onClick={lancar} disabled={isPending}>{isPending ? 'Lançando…' : 'Lançar consumo'}</Button>
          </Box>
        </>
      )}
    </Box>
  )
}

interface LinhaDaListaProps {
  consumo: ConsumoMaterial
  material?: Material
  rascunho: Rascunho
  onChange: (mudanca: Partial<Rascunho>) => void
}

function LinhaDaLista({ consumo, material, rascunho, onChange }: LinhaDaListaProps) {
  const quantidade = lerQuantidadeOpcional(rascunho.qtd)
  const informada = quantidade !== null && !Number.isNaN(quantidade)
  const desvio = informada ? quantidade - consumo.quantidadePlanejada : 0
  const temDesvio = informada && Math.abs(desvio) > 0.0001
  const nome = material ? formatarCodigoMaterial(material.codigo) : 'Material'

  return (
    <>
      <TableRow sx={ESPACO_PARA_AJUDA}>
        <TableCell>
          <Typography variant="body2" fontWeight={600}>{nome}</Typography>
          {material && <Typography variant="caption" color="text.secondary">{material.descricao}</Typography>}
        </TableCell>
        <TableCell align="right"><Typography variant="body2">{formatarQuantidade(consumo.quantidadePlanejada, consumo.unidadeDeMedida)}</Typography></TableCell>
        <TableCell>
          <TextField size="small" value={rascunho.qtd} onChange={(e) => onChange({ qtd: e.target.value })} placeholder="—"
            slotProps={{ ...AJUDA_SEM_DESLOCAR, htmlInput: { inputMode: 'decimal', 'aria-label': `Quantidade consumida de ${nome}` } }}
            helperText={temDesvio ? `Desvio: ${desvio > 0 ? '+' : ''}${formatarQuantidade(desvio, consumo.unidadeDeMedida)}` : undefined} />
        </TableCell>
        <TableCell>
          {temDesvio && (
            <TextField size="small" fullWidth value={rascunho.justificativa} onChange={(e) => onChange({ justificativa: e.target.value })}
              placeholder="Obrigatória" slotProps={{ htmlInput: { 'aria-label': `Justificativa do desvio de ${nome}` } }} />
          )}
        </TableCell>
      </TableRow>
      {informada && quantidade > 0 && (
        <TableRow>
          <TableCell colSpan={4} sx={{ bgcolor: 'grey.50' }}>
            <AlocacaoDeLotes materialId={consumo.materialId} quantidade={lerQuantidade(rascunho.qtd)} unidade={consumo.unidadeDeMedida}
              alocado={rascunho.alocado} onChange={(alocado) => onChange({ alocado })} />
          </TableCell>
        </TableRow>
      )}
    </>
  )
}

interface LinhaAdicionadaProps {
  item: ItemAdicionado
  opcoes: Material[]
  materialPorId: Map<string, Material>
  onChange: (mudanca: Partial<ItemAdicionado>) => void
  onRemover: () => void
}

function LinhaAdicionada({ item, opcoes, materialPorId, onChange, onRemover }: LinhaAdicionadaProps) {
  const material = materialPorId.get(item.materialId)
  const quantidade = lerQuantidade(item.qtd)

  return (
    <>
      <TableRow sx={ESPACO_PARA_AJUDA}>
        <TableCell>
          <TextField select size="small" fullWidth label="Material fora da lista" value={item.materialId}
            onChange={(e) => onChange({ materialId: e.target.value, alocado: {} })}>
            {opcoes.map((m) => (
              <MenuItem key={m.id} value={m.id}>{formatarCodigoMaterial(m.codigo)} — {m.descricao}</MenuItem>
            ))}
          </TextField>
        </TableCell>
        <TableCell align="right"><Typography variant="body2" color="text.secondary">0 (não planejado)</Typography></TableCell>
        <TableCell>
          <TextField size="small" value={item.qtd} onChange={(e) => onChange({ qtd: e.target.value })} placeholder="Qtd"
            slotProps={{ ...AJUDA_SEM_DESLOCAR, htmlInput: { inputMode: 'decimal', 'aria-label': 'Quantidade consumida do item adicionado' } }}
            helperText={material?.unidadeDeMedida} />
        </TableCell>
        <TableCell>
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
            <TextField size="small" fullWidth value={item.justificativa} onChange={(e) => onChange({ justificativa: e.target.value })}
              placeholder="Obrigatória" slotProps={{ htmlInput: { 'aria-label': 'Justificativa do item adicionado' } }} />
            <IconButton size="small" aria-label="Remover item adicionado" onClick={onRemover}><DeleteIcon fontSize="small" /></IconButton>
          </Box>
        </TableCell>
      </TableRow>
      {item.materialId && quantidade > 0 && (
        <TableRow>
          <TableCell colSpan={4} sx={{ bgcolor: 'grey.50' }}>
            <AlocacaoDeLotes materialId={item.materialId} quantidade={quantidade} unidade={material?.unidadeDeMedida ?? ''}
              alocado={item.alocado} onChange={(alocado) => onChange({ alocado })} />
          </TableCell>
        </TableRow>
      )}
    </>
  )
}
