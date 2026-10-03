import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import FormControlLabel from '@mui/material/FormControlLabel'
import Switch from '@mui/material/Switch'
import Typography from '@mui/material/Typography'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Chip from '@mui/material/Chip'
import Tooltip from '@mui/material/Tooltip'
import WarehouseIcon from '@mui/icons-material/WarehouseOutlined'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { useQuery } from '@tanstack/react-query'
import { consultarEstoque } from '../api/estoque'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import {
  codigoMaterialCorresponde, diasAte, formatarCodigoMaterial, formatarData, formatarDataHora, formatarQuantidade,
} from '../utils/formatacao'
import { TIPO_MATERIAL_LABEL, type TipoMaterial } from '../types'

/**
 * Estoque por material (ADR-0012): posição calculada a partir dos saldos dos lotes —
 * disponível (lotes disponíveis e válidos), indisponível (bloqueados ou vencidos),
 * próximo vencimento e última entrada. Clique leva aos lotes do material.
 */
export function EstoquePage() {
  const navigate = useNavigate()
  const [busca, setBusca] = useState('')
  const [tipo, setTipo] = useState<TipoMaterial | 'TODOS'>('TODOS')
  const [somenteComSaldo, setSomenteComSaldo] = useState(false)

  const { data: posicoes = [], isLoading, isError } = useQuery({ queryKey: ['estoque'], queryFn: consultarEstoque })

  const termo = busca.trim()
  const filtradas = posicoes.filter((p) =>
    (tipo === 'TODOS' || p.tipo === tipo)
    && (!somenteComSaldo || p.saldoDisponivel > 0)
    && (termo === '' || codigoMaterialCorresponde(p.codigo, termo) || p.descricao.toLowerCase().includes(termo.toLowerCase())))

  const vencendo = posicoes.filter((p) => p.proximoVencimento && diasAte(p.proximoVencimento) <= 30).length
  const semSaldo = posicoes.filter((p) => p.tipo === 'MATERIA_PRIMA' && p.saldoDisponivel <= 0).length

  return (
    <Box>
      <PageHeader
        titulo="Estoque"
        subtitulo={isLoading ? '…' : `${posicoes.length} materiais · saldo calculado a partir dos lotes`}
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Estoque' }]}
      />

      {isError && <Alert severity="error" sx={{ mb: 2 }}>Não foi possível carregar o estoque.</Alert>}
      {(vencendo > 0 || semSaldo > 0) && (
        <Alert severity="warning" icon={<WarningAmberIcon />} sx={{ mb: 2 }}>
          {vencendo > 0 && `${vencendo} ${vencendo === 1 ? 'material tem lote' : 'materiais têm lotes'} vencendo em até 30 dias. `}
          {semSaldo > 0 && `${semSaldo} ${semSaldo === 1 ? 'matéria-prima está' : 'matérias-primas estão'} sem saldo disponível.`}
        </Alert>
      )}

      <Card>
        <Box sx={{ p: 2, display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center', borderBottom: '1px solid', borderColor: 'divider' }}>
          <TextField
            placeholder="Buscar por código (103.000.001) ou descrição…"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            sx={{ flex: 1, minWidth: 220 }}
          />
          <TextField select label="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value as TipoMaterial | 'TODOS')}
            sx={{ minWidth: 180 }}>
            <MenuItem value="TODOS">Todos</MenuItem>
            {(Object.keys(TIPO_MATERIAL_LABEL) as TipoMaterial[]).map((t) => (
              <MenuItem key={t} value={t}>{TIPO_MATERIAL_LABEL[t]}</MenuItem>
            ))}
          </TextField>
          <FormControlLabel
            control={<Switch checked={somenteComSaldo} onChange={(e) => setSomenteComSaldo(e.target.checked)} />}
            label="Só com saldo"
          />
        </Box>

        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : filtradas.length === 0 ? (
          <EmptyState
            Icone={WarehouseIcon}
            titulo="Nenhum material encontrado"
            descricao={posicoes.length > 0
              ? 'Nenhum material atende aos filtros.'
              : 'O estoque nasce das entradas de notas fiscais e das ordens de produção concluídas.'}
          />
        ) : (
          <TableContainer>
            <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell align="right">Disponível</TableCell>
                  <TableCell align="center">Lotes</TableCell>
                  <TableCell align="right">Indisponível</TableCell>
                  <TableCell>Próx. vencimento</TableCell>
                  <TableCell>Última entrada</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtradas.map((p) => {
                  const dias = p.proximoVencimento ? diasAte(p.proximoVencimento) : null
                  const alerta = dias !== null && dias <= 30
                  return (
                    <TableRow key={p.materialId} hover onClick={() => navigate(`/estoque/${p.materialId}`)} sx={{ cursor: 'pointer' }}>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }} noWrap>
                          {formatarCodigoMaterial(p.codigo)}
                        </Typography>
                      </TableCell>
                      <TableCell><Typography variant="body2">{p.descricao}</Typography></TableCell>
                      <TableCell>
                        <Chip size="small" variant="outlined" label={TIPO_MATERIAL_LABEL[p.tipo]} sx={{ fontSize: '0.7rem' }} />
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" fontWeight={600} noWrap
                          color={p.saldoDisponivel > 0 ? 'text.primary' : 'text.disabled'}>
                          {formatarQuantidade(p.saldoDisponivel, p.unidadeDeMedida)}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">{p.lotesDisponiveis}</TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" noWrap color={p.saldoIndisponivel > 0 ? 'warning.main' : 'text.disabled'}>
                          {formatarQuantidade(p.saldoIndisponivel, p.unidadeDeMedida)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {p.proximoVencimento ? (
                          <Tooltip title={alerta ? `Vence em ${dias} dias` : ''}>
                            <Typography variant="body2" noWrap color={alerta ? 'warning.main' : 'text.secondary'}
                              fontWeight={alerta ? 600 : 400}>
                              {formatarData(p.proximoVencimento)}
                            </Typography>
                          </Tooltip>
                        ) : <Typography variant="body2" color="text.disabled">—</Typography>}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary" noWrap>{formatarDataHora(p.ultimaEntrada)}</Typography>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>
    </Box>
  )
}
