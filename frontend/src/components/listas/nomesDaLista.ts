import type { ItemListaTecnica, ListaTecnica, Material } from '../../types'

/** Código e descrição de um material citado numa lista técnica. */
export interface NomeDoMaterial {
  codigo?: string
  descricao?: string
}

function nome(codigo: string | null | undefined, descricao: string | null | undefined, doCatalogo?: Material): NomeDoMaterial | undefined {
  const resultado = { codigo: codigo ?? doCatalogo?.codigo, descricao: descricao ?? doCatalogo?.descricao }
  return resultado.codigo || resultado.descricao ? resultado : undefined
}

/**
 * O material produzido pela lista: a resposta já traz código e descrição, e o catálogo só completa o que faltar. Assim
 * quem vê listas sem MATERIAL_VISUALIZAR enxerga nomes, não ids internos.
 */
export function nomeDoMaterialDaLista(lista: ListaTecnica, materialPorId: Map<string, Material>): NomeDoMaterial | undefined {
  return nome(lista.materialCodigo, lista.materialDescricao, materialPorId.get(lista.materialId))
}

/** O componente de um item da lista, pela mesma regra. */
export function nomeDoComponente(item: ItemListaTecnica, materialPorId: Map<string, Material>): NomeDoMaterial | undefined {
  return nome(item.componenteCodigo, item.componenteDescricao, materialPorId.get(item.materialComponenteId))
}
