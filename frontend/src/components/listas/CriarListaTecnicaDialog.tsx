import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutlineOutlined'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cadastrarListaTecnica, listarListasTecnicas } from '../../api/listasTecnicas'
import { usePermissoes } from '../../hooks/usePermissoes'
import { listarMateriais } from '../../api/materiais'
import type { CadastrarListaTecnicaRequest, ListaTecnica, TipoMaterial } from '../../types'
import { formatarCodigoMaterial } from '../../utils/formatacao'
import { mensagemDeErroDaApi } from '../../utils/mensagensDeErro'
import { sugerirProximaVersao } from '../../utils/versoes'
import { useSincronizarAoMudar } from '../../hooks/useSincronizarAoMudar'
import { CampoUnidadeDeMedida } from '../CampoUnidadeDeMedida'
import { diferencaDaSoma, mensagemDaSoma } from '../../utils/unidades'

interface ItemForm { materialComponenteId: string; quantidadePlanejada: string; unidadeDeMedida: string }

const ITEM_VAZIO: ItemForm = { materialComponenteId: '', quantidadePlanejada: '', unidadeDeMedida: 'UN' }

interface CriarListaTecnicaDialogProps {
  aberto: boolean
  onFechar: () => void
  /**
   * Versão a copiar: o material fica fixo (a nova versão entra na lista dele), os componentes
   * vêm preenchidos para ajustar e o rótulo da versão é sugerido.
   */
  modelo?: ListaTecnica
  /** Rótulos das versões que o material já tem, para sugerir o próximo ("v3"). */
  versoesExistentes?: string[]
  /** Chamado depois de criar, com o material e o rótulo da versão nova. */
  onCriada?: (materialId: string, versao: string) => void
}

/** Cadastro de uma versão de lista técnica, do zero ou copiando outra versão do mesmo material. */
export function CriarListaTecnicaDialog({ aberto, onFechar, modelo, versoesExistentes = [], onCriada }: CriarListaTecnicaDialogProps) {
  const [materialId, setMaterialId] = useState('')
  const [versao, setVersao] = useState('')
  const [base, setBase] = useState('1')
  const [itens, setItens] = useState<ItemForm[]>([ITEM_VAZIO])
  // Verdadeiro depois que a pessoa mexe na versão: a sugestão não sobrescreve o que ela digitou.
  const [versaoDigitada, setVersaoDigitada] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()
  const { tem } = usePermissoes()

  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais, enabled: aberto })
  const produtivos = materiais.filter((m) => (m.tipo as TipoMaterial) !== 'MATERIA_PRIMA')
  // Regra inegociável (ADR-0024): produto acabado só é componente de lista de semiacabado (a conversão).
  const donoEhSemiacabado = materiais.find((m) => m.id === materialId)?.tipo === 'SEMIACABADO'
  const donoUnidade = materiais.find((m) => m.id === materialId)?.unidadeDeMedida ?? 'un'
  const componentesPermitidos = donoEhSemiacabado ? materiais : materiais.filter((m) => m.tipo !== 'PRODUTO_ACABADO')

  // Versão sugerida para um material que já tem versões: a próxima ("v3"), ou "v1" se ainda não tem nenhuma. Só com a
  // permissão de ver todas as listas: quem só vê as ativas não enxerga as em revisão e poderia receber uma versão que já existe.
  // Ao copiar uma versão, a sugestão vem das versões já conhecidas pela tela de origem (efeito de abertura, abaixo).
  const { data: versoesDoMaterial } = useQuery({
    queryKey: ['listas-tecnicas', materialId],
    queryFn: () => listarListasTecnicas(materialId),
    enabled: aberto && !modelo && !!materialId && tem('LISTA_VISUALIZAR_TODAS'),
  })
  const proximaVersao = modelo || !versoesDoMaterial ? ''
    : versoesDoMaterial.length === 0 ? 'v1' : sugerirProximaVersao(versoesDoMaterial.map((l) => l.versao))
  useSincronizarAoMudar([materialId, proximaVersao], () => {
    if (modelo || !materialId || versaoDigitada) return
    setVersao(proximaVersao)
  })

  // Só um aviso (ADR-0037): quando o material e os componentes são da mesma grandeza (peso com peso), mostra se a soma
  // dos componentes difere da quantidade base. Não bloqueia nem grava nada.
  const quantidadeBaseNumero = Number(base.trim().replace(',', '.'))
  const diferencaDaSomaDosComponentes = materialId ? diferencaDaSoma(
    itens.filter((i) => i.materialComponenteId && i.quantidadePlanejada)
      .map((i) => ({ quantidade: Number(String(i.quantidadePlanejada).replace(',', '.')), unidadeDeMedida: i.unidadeDeMedida })),
    quantidadeBaseNumero,
    donoUnidade,
  ) : null

  // Cada abertura recomeça: vazio, ou preenchido a partir do modelo.
  useSincronizarAoMudar([aberto, modelo?.id], () => {
    if (!aberto) return
    setErro(null)
    setVersaoDigitada(false)
    if (modelo) {
      setMaterialId(modelo.materialId)
      setVersao(sugerirProximaVersao(versoesExistentes))
      setBase(String(modelo.quantidadeBase ?? 1))
      setItens(modelo.itens.map((i) => ({
        materialComponenteId: i.materialComponenteId,
        quantidadePlanejada: String(i.quantidadePlanejada),
        unidadeDeMedida: i.unidadeDeMedida,
      })))
    } else {
      setMaterialId('')
      setVersao('')
      setBase('1')
      setItens([ITEM_VAZIO])
    }
  })

  const { mutate, isPending } = useMutation({
    mutationFn: (payload: CadastrarListaTecnicaRequest) => cadastrarListaTecnica(payload),
    onSuccess: (_resultado, payload) => {
      queryClient.invalidateQueries({ queryKey: ['listas-tecnicas-todas'] })
      queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] })
      queryClient.invalidateQueries({ queryKey: ['unidades'] })
      onFechar()
      onCriada?.(payload.materialId, payload.versao)
    },
    onError: (err: unknown) => {
      setErro(mensagemDeErroDaApi(err, 'Não foi possível cadastrar a lista técnica. Tente novamente.'))
    },
  })

  const handleSubmit = () => {
    if (!materialId || !versao.trim()) { setErro('Material e versão são obrigatórios.'); return }
    const quantidadeBase = Number(base.trim().replace(',', '.'))
    if (!Number.isFinite(quantidadeBase) || quantidadeBase <= 0) { setErro('Informe a quantidade base: um número maior que zero.'); return }
    if (!/^\d{1,8}([.,]\d{1,4})?$/.test(base.trim())) { setErro('A quantidade base aceita até 8 dígitos inteiros e 4 casas decimais.'); return }
    const itensValidos = itens.filter((i) => i.materialComponenteId && i.quantidadePlanejada)
    if (itensValidos.length === 0) { setErro('Adicione ao menos um componente.'); return }
    mutate({
      materialId,
      versao: versao.trim(),
      quantidadeBase,
      itens: itensValidos.map((i) => ({
        materialComponenteId: i.materialComponenteId,
        quantidadePlanejada: Number(i.quantidadePlanejada),
        unidadeDeMedida: i.unidadeDeMedida,
      })),
    })
  }

  const addItem = () => setItens((prev) => [...prev, ITEM_VAZIO])
  const removeItem = (idx: number) => setItens((prev) => prev.filter((_, i) => i !== idx))
  const updateItem = (idx: number, campo: keyof ItemForm, valor: string) =>
    setItens((prev) => prev.map((item, i) => (i === idx ? { ...item, [campo]: valor } : item)))

  return (
    <Dialog open={aberto} onClose={onFechar} fullWidth maxWidth="md">
      <DialogTitle>{modelo ? `Nova versão a partir da ${modelo.versao}` : 'Nova lista técnica (BOM)'}</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        {modelo && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Os componentes da versão {modelo.versao} foram copiados. Ajuste o que mudou; a nova versão nasce
            em revisão e só é usada nas ordens depois de ativada.
          </Alert>
        )}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid size={{ xs: 12, sm: 8 }}>
            <TextField
              select label="Material *" fullWidth value={materialId}
              onChange={(e) => {
                const novo = materiais.find((m) => m.id === e.target.value)
                setMaterialId(e.target.value)
                // Trocar para um material que não é semiacabado tira os produtos acabados que já estavam escolhidos.
                if (novo?.tipo !== 'SEMIACABADO') {
                  setItens((prev) => prev.map((i) => (materiais.find((m) => m.id === i.materialComponenteId)?.tipo === 'PRODUTO_ACABADO' ? { ...i, materialComponenteId: '' } : i)))
                }
              }}
              disabled={!!modelo}
              helperText={modelo ? 'A nova versão entra na lista deste material.' : 'Produto acabado ou semiacabado que será fabricado.'}
            >
              {produtivos.map((m) => (
                <MenuItem key={m.id} value={m.id}>{formatarCodigoMaterial(m.codigo)} — {m.descricao}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Quantidade base *" value={base} onChange={(e) => setBase(e.target.value)} fullWidth
              slotProps={{ htmlInput: { inputMode: 'decimal' } }}
              helperText={`Os componentes valem para produzir ${base.trim() || '…'} ${donoUnidade}.`} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Versão *" value={versao} onChange={(e) => { setVersaoDigitada(true); setVersao(e.target.value) }} fullWidth placeholder="v1, v2-rev-a…"
              helperText={proximaVersao ? `Próxima versão deste material: ${proximaVersao}.` : undefined} slotProps={{ htmlInput: { maxLength: 50 } }} />
          </Grid>
        </Grid>

        <Divider sx={{ my: 2.5 }} />

        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
          <Box>
            <Typography variant="subtitle2">Componentes</Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
              {donoEhSemiacabado
                ? 'Esta lista pode consumir produto acabado (conversão); a ordem precisará ser do tipo Conversão.'
                : 'Matéria-prima e semiacabado. Produto acabado só entra em lista de semiacabado (conversão).'}
            </Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
              Os componentes somados devem corresponder à quantidade base. Ex.: para 1 KG de bolo, 200 G de fermento,
              600 G de massa e 200 G de farinha.
            </Typography>
          </Box>
          <Button size="small" startIcon={<AddCircleOutlineIcon />} onClick={addItem}>Adicionar</Button>
        </Box>

        {itens.map((item, idx) => (
          <Box key={idx} sx={{ display: 'flex', gap: 1.5, mb: 1.5, alignItems: 'flex-start' }}>
            <TextField select label="Material componente" value={item.materialComponenteId} onChange={(e) => updateItem(idx, 'materialComponenteId', e.target.value)} sx={{ flex: 2 }}>
              {componentesPermitidos.map((m) => (
                <MenuItem key={m.id} value={m.id}>{formatarCodigoMaterial(m.codigo)} — {m.descricao}</MenuItem>
              ))}
            </TextField>
            <TextField label="Qtd" type="number" value={item.quantidadePlanejada} onChange={(e) => updateItem(idx, 'quantidadePlanejada', e.target.value)} sx={{ width: 90 }} slotProps={{ htmlInput: { min: 0.0001, step: 0.0001 } }} />
            <CampoUnidadeDeMedida label="Un." value={item.unidadeDeMedida} onChange={(unidade) => updateItem(idx, 'unidadeDeMedida', unidade)} largura={120} />
            <IconButton aria-label={`Remover componente ${idx + 1}`} onClick={() => removeItem(idx)} disabled={itens.length === 1} sx={{ mt: 0.5 }}>
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Box>
        ))}

        {diferencaDaSomaDosComponentes && (
          <Alert severity="warning" sx={{ mt: 1 }}>
            {mensagemDaSoma(diferencaDaSomaDosComponentes, quantidadeBaseNumero)} Confira as quantidades; você ainda pode criar a lista.
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isPending ? 'Salvando…' : modelo ? 'Criar versão' : 'Criar lista'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
