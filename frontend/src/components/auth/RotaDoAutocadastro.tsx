import { Navigate } from 'react-router-dom'
import { CadastroPage } from '../../pages/CadastroPage'
import { autocadastroHabilitado } from '../../utils/autocadastro'

/** A tela de cadastro só existe onde o autocadastro está ligado; nos demais ambientes, a rota leva ao login. */
export function RotaDoAutocadastro() {
  return autocadastroHabilitado() ? <CadastroPage /> : <Navigate to="/login" replace />
}
