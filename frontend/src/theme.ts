import { createTheme, alpha } from '@mui/material/styles'
import { ptBR } from '@mui/material/locale'

export const theme = createTheme(
  {
    palette: {
      primary: {
        main: '#1565c0',
        dark: '#003c8f',
        light: '#4f83cc',
      },
      secondary: {
        main: '#e65100',
      },
      success: {
        main: '#2e7d32',
        light: '#4caf50',
      },
      warning: {
        main: '#ed6c02',
        light: '#ff9800',
      },
      error: {
        main: '#d32f2f',
        light: '#ef5350',
      },
      info: {
        main: '#0288d1',
      },
      background: {
        default: '#f0f2f5',
        paper: '#ffffff',
      },
      grey: {
        50: '#f8f9fa',
        100: '#f1f3f5',
      },
    },
    typography: {
      fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
      h4: { fontWeight: 700 },
      h5: { fontWeight: 700 },
      h6: { fontWeight: 600 },
      subtitle1: { fontWeight: 600 },
      subtitle2: { fontWeight: 600 },
    },
    shape: {
      borderRadius: 8,
    },
    components: {
      MuiCard: {
        defaultProps: { elevation: 0 },
        styleOverrides: {
          root: {
            border: '1px solid rgba(0,0,0,0.08)',
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: { textTransform: 'none', fontWeight: 600 },
          contained: { boxShadow: 'none', '&:hover': { boxShadow: 'none' } },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { fontWeight: 500 },
        },
      },
      MuiTableHead: {
        styleOverrides: {
          root: {
            '& .MuiTableCell-head': {
              fontWeight: 600,
              fontSize: '0.75rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              color: 'rgba(0,0,0,0.55)',
              backgroundColor: '#f8f9fa',
              borderBottom: '1px solid rgba(0,0,0,0.08)',
              paddingTop: 10,
              paddingBottom: 10,
            },
          },
        },
      },
      MuiTableRow: {
        styleOverrides: {
          root: {
            '&:hover': { backgroundColor: alpha('#1565c0', 0.03) },
            '&:last-child td': { border: 0 },
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: {
            borderBottom: '1px solid rgba(0,0,0,0.06)',
            paddingTop: 10,
            paddingBottom: 10,
          },
        },
      },
      MuiTextField: {
        defaultProps: { size: 'small' },
      },
      MuiInputBase: {
        styleOverrides: {
          root: { fontSize: '0.875rem' },
        },
      },
      MuiListSubheader: {
        styleOverrides: {
          root: {
            fontSize: '0.65rem',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            color: 'rgba(0,0,0,0.4)',
            lineHeight: '2rem',
            paddingLeft: 16,
          },
        },
      },
    },
  },
  ptBR,
)
