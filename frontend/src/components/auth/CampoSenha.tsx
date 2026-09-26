import { useState } from 'react'
import TextField, { type TextFieldProps } from '@mui/material/TextField'
import InputAdornment from '@mui/material/InputAdornment'
import IconButton from '@mui/material/IconButton'
import LockIcon from '@mui/icons-material/Lock'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'

/**
 * Campo de senha com ícone e botão de mostrar/ocultar.
 * Aceita as mesmas props do TextField (label, value, error, helperText...).
 */
export function CampoSenha(props: Omit<TextFieldProps, 'type'>) {
  const [visivel, setVisivel] = useState(false)

  return (
    <TextField
      {...props}
      type={visivel ? 'text' : 'password'}
      fullWidth
      margin="normal"
      slotProps={{
        input: {
          startAdornment: (
            <InputAdornment position="start">
              <LockIcon color="action" fontSize="small" />
            </InputAdornment>
          ),
          endAdornment: (
            <InputAdornment position="end">
              <IconButton
                onClick={() => setVisivel((v) => !v)}
                edge="end"
                size="small"
                aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
              >
                {visivel ? <VisibilityOffIcon /> : <VisibilityIcon />}
              </IconButton>
            </InputAdornment>
          ),
        },
      }}
    />
  )
}
