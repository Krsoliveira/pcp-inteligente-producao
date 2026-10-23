import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { retirarListaTecnicaDeUso } from '../../api/listasTecnicas'
import type { ListaTecnica } from '../../types'
import { mensagemDeErroDaApi } from '../../utils/mensagensDeErro'

interface RetirarDeUsoDialogProps {
  /** Versão a retirar; `null` mantém o diálogo fechado. */
  lista: ListaTecnica | null
  /** Como o material aparece para o usuário, ex.: "103.000.001 — Viga metálica". */
  nomeDoMaterial: string
  onFechar: () => void
}

/** Confirma antes de retirar uma versão de uso: a versão fica obsoleta e não volta a ser ativada. */
export function RetirarDeUsoDialog({ lista, nomeDoMaterial, onFechar }: RetirarDeUsoDialogProps) {
  const queryClient = useQueryClient()
  const [erro, setErro] = useState<string | null>(null)

  const { mutate, isPending } = useMutation({
    mutationFn: (id: string) => retirarListaTecnicaDeUso(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] })
      setErro(null)
      onFechar()
    },
    onError: (e: unknown) => setErro(mensagemDeErroDaApi(e, 'Não foi possível retirar a versão de uso.')),
  })

  const fechar = () => { setErro(null); onFechar() }

  return (
    <Dialog open={lista !== null} onClose={isPending ? undefined : fechar} fullWidth maxWidth="xs">
      <DialogTitle>Retirar a versão {lista?.versao} de uso?</DialogTitle>
      <DialogContent>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <DialogContentText>
          {nomeDoMaterial}, versão {lista?.versao}. Ela deixa de aparecer ao criar ordens e <strong>não pode
          ser ativada de novo</strong>. As ordens que já usam essa versão não são afetadas. As outras
          versões do material continuam como estão.
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={fechar} disabled={isPending}>Cancelar</Button>
        <Button color="warning" variant="contained" onClick={() => lista && mutate(lista.id)} disabled={isPending}>
          {isPending ? 'Retirando…' : 'Retirar de uso'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
