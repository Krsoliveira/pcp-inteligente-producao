import { createPortal } from 'react-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import GlobalStyles from '@mui/material/GlobalStyles'
import Typography from '@mui/material/Typography'
import PrintIcon from '@mui/icons-material/PrintOutlined'
import { useQuery } from '@tanstack/react-query'
import { QRCodeSVG } from 'qrcode.react'
import { buscarOrdemPorId } from '../../api/ordens'
import { formatarCodigoMaterial, formatarData, formatarQuantidade } from '../../utils/formatacao'
import type { Lote, Material } from '../../types'

/** Classe da área que vai para a impressora; todo o resto da página some na impressão. */
const AREA_DE_IMPRESSAO = 'etiqueta-area-de-impressao'

/** Endereço que o QR code abre: a tela do lote no próprio sistema (pede login; ADR-0028). */
export function enderecoDoLote(loteId: string): string {
  return `${window.location.origin}/lotes/${loteId}`
}

interface EtiquetaProps {
  lote: Lote
  material: Material | undefined
}

/** Etiqueta de 100 × 50 mm: QR code do lote à esquerda, dados para leitura humana à direita. */
function Etiqueta({ lote, material }: EtiquetaProps) {
  const ordemQuery = useQuery({
    queryKey: ['ordem', lote.ordemProducaoId],
    queryFn: () => buscarOrdemPorId(lote.ordemProducaoId!),
    enabled: lote.origem === 'PRODUCAO' && !!lote.ordemProducaoId,
  })
  // A nota fiscal ou a ordem nunca é cortada: é o elo da rastreabilidade. Só o nome do fornecedor pode ser abreviado.
  const origem = lote.origem === 'COMPRA'
    ? (lote.notaFiscal ? `NF ${lote.notaFiscal}` : 'Lote de compra')
    : ordemQuery.data ? `Ordem ${ordemQuery.data.codigo}` : 'Lote de produção'

  return (
    <Box sx={{ width: '100mm', height: '50mm', boxSizing: 'border-box', p: '3mm', display: 'flex', gap: '3mm',
      bgcolor: '#fff', color: '#000', fontFamily: 'Arial, Helvetica, sans-serif', overflow: 'hidden' }}>
      <Box sx={{ flexShrink: 0, width: '44mm', height: '44mm' }}>
        <QRCodeSVG value={enderecoDoLote(lote.id)} level="M" marginSize={0} title={`QR code do lote ${lote.numeroLote}`}
          style={{ width: '100%', height: '100%' }} />
      </Box>
      <Box sx={{ minWidth: 0, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
        <Box>
          <Box sx={{ fontSize: '7pt', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Lote</Box>
          <Box sx={{ fontSize: '14pt', fontWeight: 700, fontFamily: 'monospace', lineHeight: 1.1 }}>{lote.numeroLote}</Box>
        </Box>
        <Box sx={{ fontSize: '8pt', lineHeight: 1.25 }}>
          <Box sx={{ fontWeight: 700 }}>{material ? formatarCodigoMaterial(material.codigo) : '—'}</Box>
          <Box sx={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
            {material?.descricao ?? ''}
          </Box>
        </Box>
        <Box sx={{ fontSize: '8pt', lineHeight: 1.35 }}>
          <Box>Qtd: <b>{formatarQuantidade(lote.quantidade, lote.unidadeDeMedida)}</b></Box>
          <Box>Fab.: {formatarData(lote.dataFabricacao)} · Val.: <b>{formatarData(lote.dataValidade)}</b></Box>
          <Box sx={{ fontWeight: 700 }}>{origem}</Box>
          {lote.origem === 'COMPRA' && lote.fornecedor && (
            <Box sx={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{lote.fornecedor}</Box>
          )}
        </Box>
      </Box>
    </Box>
  )
}

interface EtiquetaDoLoteDialogProps extends EtiquetaProps {
  aberto: boolean
  onFechar: () => void
}

/**
 * Prévia e impressão da etiqueta do lote (ADR-0028). A impressão usa o diálogo do navegador: a página
 * vira uma etiqueta de 100 × 50 mm, para impressora térmica ou comum, e só a etiqueta sai.
 */
export function EtiquetaDoLoteDialog({ lote, material, aberto, onFechar }: EtiquetaDoLoteDialogProps) {
  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="sm">
      <DialogTitle>Etiqueta do lote</DialogTitle>
      <DialogContent>
        <Typography
          variant="body2"
          sx={{ color: 'text.secondary', mb: 2 }}>
          O QR code abre este lote no sistema (com login). Tamanho: 100 × 50 mm.
        </Typography>
        <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, width: 'fit-content', mx: 'auto' }}>
          <Etiqueta lote={lote} material={material} />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onFechar}>Fechar</Button>
        <Button variant="contained" startIcon={<PrintIcon />} onClick={() => window.print()}>Imprimir</Button>
      </DialogActions>

      {aberto && (
        <>
          <GlobalStyles styles={{
            [`.${AREA_DE_IMPRESSAO}`]: { display: 'none' },
            '@media print': {
              '@page': { size: '100mm 50mm', margin: 0 },
              'body > *': { display: 'none !important' },
              [`body > .${AREA_DE_IMPRESSAO}`]: { display: 'block !important' },
            },
          }} />
          {createPortal(<div className={AREA_DE_IMPRESSAO}><Etiqueta lote={lote} material={material} /></div>, document.body)}
        </>
      )}
    </Dialog>
  )
}
