/**
 * Sugere o rótulo da próxima versão de uma lista técnica: se as versões existentes seguem o padrão
 * "v1", "v2"…, devolve a seguinte ao maior número ("v3"); sem nenhuma nesse padrão, devolve vazio
 * para o usuário digitar. Rótulos livres ("v2-tolerancia-extra") são ignorados na contagem.
 */
export function sugerirProximaVersao(versoesExistentes: string[]): string {
  const numeros = versoesExistentes
    .map((v) => /^v(\d+)$/i.exec(v.trim()))
    .filter((m): m is RegExpExecArray => m !== null)
    .map((m) => Number(m[1]))
  return numeros.length === 0 ? '' : `v${Math.max(...numeros) + 1}`
}
