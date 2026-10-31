import { useState, useEffect } from 'react'
import { usePermissoes } from '../hooks/usePermissoes'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Grid from '@mui/material/Grid'
import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Divider from '@mui/material/Divider'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { criarOrdem } from '../api/ordens'
import { listarMateriais } from '../api/materiais'
import { listarListasTecnicas } from '../api/listasTecnicas'
import { listarTiposOrdem } from '../api/tiposOrdem'
import { formatarCodigoMaterial } from '../utils/formatacao'
import type { CriarOrdemRequest } from '../types'
import { mensagemDeErroDaApi } from '../utils/mensagensDeErro'

interface CriarOrdemDialogProps {
  aberto: boolean
  onFechar: () => void
}

const CENTROS_DE_TRABALHO = [
  'Usinagem CNC',
  'Montagem',
  'Soldagem MIG/TIG',
  'Pintura Industrial',
  'Inspeção de Qualidade',
  'Fundição Sob Pressão',
  'Estamparia',
  'Tratamento Térmico',
]

const VAZIO: CriarOrdemRequest = {
  codigo: '',
  materialId: '',
  listaTecnicaId: '',
  tipoOrdemId: null,
  centroDeTrabalho: '',
  quantidade: 1,
  inicioPlanejado: '',
  fimPlanejado: '',
}

export function CriarOrdemDialog({ aberto, onFechar }: CriarOrdemDialogProps) {
  const [form, setForm] = useState<CriarOrdemRequest>(VAZIO)
  const { tem } = usePermissoes()
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: materiais = [], isLoading: carregandoMateriais } = useQuery({
    queryKey: ['materiais'],
    queryFn: listarMateriais,
    enabled: aberto,
    select: (lista) => lista.filter((m) => m.tipo !== 'MATERIA_PRIMA'),
  })

  // O tipo da ordem é opcional: quem não pode consultar os tipos nem vê o campo (o servidor recusaria a consulta).
  const podeVerTipos = tem('TIPO_ORDEM_VISUALIZAR')
  const { data: tiposOrdem = [] } = useQuery({
    queryKey: ['tipos-ordem'],
    queryFn: listarTiposOrdem,
    enabled: aberto && podeVerTipos,
  })

  // Tipo Conversão (ADR-0024): só produz semiacabado; produto acabado não aparece como material da ordem.
  const tipoSelecionado = tiposOrdem.find((t) => t.id === form.tipoOrdemId)
  const conversao = tipoSelecionado?.conversao === true
  const materiaisDaOrdem = conversao ? materiais.filter((m) => m.tipo === 'SEMIACABADO') : materiais

  const { data: listasTecnicas = [] } = useQuery({
    queryKey: ['listas-tecnicas', form.materialId],
    queryFn: () => listarListasTecnicas(form.materialId),
    enabled: aberto && !!form.materialId,
  })

  // Só versões em uso (ATIVA) podem ser escolhidas; o servidor recusa as demais (ADR-0019).
  const versoesEmUso = listasTecnicas.filter((l) => l.status === 'ATIVA')

  // Com uma única versão em uso ela já vem selecionada; com mais de uma, o usuário escolhe. A
  // dependência é o id (texto), não o array: com a consulta desativada, `data = []` cria um array
  // novo a cada render e o efeito entraria em loop (setForm → render → efeito...), travando a página.
  const versaoUnicaId = versoesEmUso.length === 1 ? versoesEmUso[0].id : ''
  useEffect(() => {
    setForm((prev) => (prev.listaTecnicaId === versaoUnicaId ? prev : { ...prev, listaTecnicaId: versaoUnicaId }))
  }, [versaoUnicaId])

  const { mutate, isPending } = useMutation({
    mutationFn: criarOrdem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ordens'] })
      setForm(VAZIO)
      setErro(null)
      onFechar()
    },
    onError: (error: unknown) => {
      setErro(mensagemDeErroDaApi(error, 'Não foi possível criar a ordem. Confira os campos e tente novamente.'))
    },
  })

  const set = (campo: keyof CriarOrdemRequest) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setErro(null)
    const valor = campo === 'quantidade' ? Number(e.target.value) : e.target.value || null
    if (campo === 'materialId') {
      setForm((prev) => ({ ...prev, materialId: e.target.value, listaTecnicaId: '' }))
    } else {
      setForm((prev) => ({ ...prev, [campo]: valor }))
    }
  }

  const handleSubmit = () => {
    if (form.materialId && !form.listaTecnicaId && versoesEmUso.length > 1) {
      setErro('Este material tem mais de uma versão da lista técnica em uso: escolha qual usar nesta ordem.')
      return
    }
    if (!form.codigo || !form.materialId || !form.listaTecnicaId ||
        !form.centroDeTrabalho || !form.inicioPlanejado || !form.fimPlanejado) {
      setErro('Preencha todos os campos obrigatórios.')
      return
    }
    mutate(form)
  }

  const handleFechar = () => {
    setForm(VAZIO)
    setErro(null)
    onFechar()
  }

  return (
    <Dialog open={aberto} onClose={handleFechar} fullWidth maxWidth="sm">
      <DialogTitle sx={{ pb: 1 }}>Nova Ordem de Produção</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}

        <Grid container spacing={2} sx={{ mt: 0 }}>
          {/* Identificação */}
          <Grid size={12}>
            <Typography
              variant="caption"
              sx={{ color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Identificação
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Código *" value={form.codigo} onChange={set('codigo')} fullWidth placeholder="OP-0001" slotProps={{ htmlInput: { maxLength: 30 } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Quantidade *" type="number" value={form.quantidade} onChange={set('quantidade')} fullWidth slotProps={{ htmlInput: { min: 1 } }} />
          </Grid>
          {podeVerTipos && (
            <Grid size={12}>
              <TextField
                select
                label="Tipo de Ordem (opcional)"
                value={form.tipoOrdemId ?? ''}
                onChange={(e) => {
                  const novo = tiposOrdem.find((t) => t.id === e.target.value)
                  setForm((p) => {
                    // Conversão só produz semiacabado: se o material escolhido não é, a escolha é desfeita.
                    const materialAtual = materiais.find((m) => m.id === p.materialId)
                    const desfaz = novo?.conversao === true && materialAtual?.tipo !== 'SEMIACABADO'
                    return { ...p, tipoOrdemId: e.target.value || null, ...(desfaz ? { materialId: '', listaTecnicaId: '' } : {}) }
                  })
                }}
                fullWidth
                helperText={conversao
                  ? 'Conversão: consome produto acabado e produz semiacabado.'
                  : 'Categorize a ordem: Produção, Manutenção, Revenda, etc. Lista que consome produto acabado exige o tipo Conversão.'}
              >
                <MenuItem value=""><em>Sem categoria</em></MenuItem>
                {tiposOrdem.map((t) => (
                  <MenuItem key={t.id} value={t.id}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: t.cor, flexShrink: 0 }} />
                      {t.nome}
                    </Box>
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          )}

          <Grid size={12}><Divider /></Grid>

          {/* Produto */}
          <Grid size={12}>
            <Typography
              variant="caption"
              sx={{ color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Produto e Lista Técnica
            </Typography>
          </Grid>
          <Grid size={12}>
            {carregandoMateriais ? (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CircularProgress size={18} />
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>Carregando materiais…</Typography>
              </Box>
            ) : (
              <TextField
                select
                label="Material *"
                value={form.materialId}
                onChange={set('materialId')}
                fullWidth
                helperText={materiaisDaOrdem.length === 0 ? (conversao ? 'Nenhum semiacabado cadastrado.' : 'Cadastre um material primeiro.') : conversao ? 'Só semiacabados: é o que a conversão produz.' : ''}
              >
                {materiaisDaOrdem.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>{formatarCodigoMaterial(m.codigo)}</Typography>
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>{m.descricao}</Typography>
                    </Box>
                  </MenuItem>
                ))}
              </TextField>
            )}
          </Grid>
          <Grid size={12}>
            <TextField
              select
              label="Versão da Lista Técnica (BOM) *"
              value={form.listaTecnicaId}
              onChange={set('listaTecnicaId')}
              fullWidth
              disabled={!form.materialId || versoesEmUso.length === 0}
              helperText={
                form.materialId && versoesEmUso.length === 0
                  ? 'Nenhuma versão da lista técnica em uso para este material.'
                  : versoesEmUso.length > 1
                    ? `Este material tem ${versoesEmUso.length} versões em uso: escolha qual usar nesta ordem.`
                    : ''
              }
            >
              {versoesEmUso.map((l) => (
                <MenuItem key={l.id} value={l.id}>
                  {l.versao}
                  <Typography
                    component="span"
                    variant="caption"
                    sx={{ color: 'text.secondary', ml: 1 }}>
                    ({l.itens.length} {l.itens.length === 1 ? 'componente' : 'componentes'})
                  </Typography>
                </MenuItem>
              ))}
            </TextField>
          </Grid>

          <Grid size={12}><Divider /></Grid>

          {/* Execução */}
          <Grid size={12}>
            <Typography
              variant="caption"
              sx={{ color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Execução
            </Typography>
          </Grid>
          <Grid size={12}>
            <TextField
              select
              label="Centro de Trabalho *"
              value={form.centroDeTrabalho}
              onChange={set('centroDeTrabalho')}
              fullWidth
            >
              {CENTROS_DE_TRABALHO.map((centro) => (
                <MenuItem key={centro} value={centro}>{centro}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Início Planejado *" type="date" value={form.inicioPlanejado} onChange={set('inicioPlanejado')} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Fim Planejado *" type="date" value={form.fimPlanejado} onChange={set('fimPlanejado')} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={handleFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isPending ? 'Salvando…' : 'Criar Ordem'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
