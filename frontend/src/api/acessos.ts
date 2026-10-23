import { apiClient } from './client'
import type {
  CriarUsuarioRequest,
  PerfilAcesso,
  PermissaoDoCatalogo,
  SalvarPerfilRequest,
  UsuarioAcesso,
  UsuarioLogado,
} from '../types'

/** Quem está logado e o que pode fazer. */
export const consultarUsuarioLogado = async (): Promise<UsuarioLogado> => {
  const { data } = await apiClient.get<UsuarioLogado>('/v1/acessos/eu')
  return data
}

export const listarPermissoes = async (): Promise<PermissaoDoCatalogo[]> => {
  const { data } = await apiClient.get<PermissaoDoCatalogo[]>('/v1/acessos/permissoes')
  return data
}

export const listarPerfis = async (): Promise<PerfilAcesso[]> => {
  const { data } = await apiClient.get<PerfilAcesso[]>('/v1/acessos/perfis')
  return data
}

export const criarPerfil = async (payload: SalvarPerfilRequest): Promise<PerfilAcesso> => {
  const { data } = await apiClient.post<PerfilAcesso>('/v1/acessos/perfis', payload)
  return data
}

export const atualizarPerfil = async (id: string, payload: SalvarPerfilRequest): Promise<PerfilAcesso> => {
  const { data } = await apiClient.put<PerfilAcesso>(`/v1/acessos/perfis/${id}`, payload)
  return data
}

export const excluirPerfil = async (id: string): Promise<void> => {
  await apiClient.delete(`/v1/acessos/perfis/${id}`)
}

export const listarUsuarios = async (): Promise<UsuarioAcesso[]> => {
  const { data } = await apiClient.get<UsuarioAcesso[]>('/v1/acessos/usuarios')
  return data
}

export const criarUsuario = async (payload: CriarUsuarioRequest): Promise<UsuarioAcesso> => {
  const { data } = await apiClient.post<UsuarioAcesso>('/v1/acessos/usuarios', payload)
  return data
}

export const atribuirPerfil = async (usuarioId: string, perfilId: string): Promise<UsuarioAcesso> => {
  const { data } = await apiClient.patch<UsuarioAcesso>(`/v1/acessos/usuarios/${usuarioId}/perfil`, { perfilId })
  return data
}
