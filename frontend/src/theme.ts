import { createTheme, alpha } from '@mui/material/styles'
import { ptBR } from '@mui/material/locale'

export const theme = createTheme(
  {
    palette: {
      primary: {
        main: '#4F46E5',
        dark: '#3730A3',
        light: '#818CF8',
      },
      secondary: {
        main: '#0F766E',
      },
      success: {
        main: '#059669',
      },
      warning: {
        main: '#D97706',
      },
      error: {
        main: '#DC2626',
      },
      info: {
        main: '#2563EB',
      },
      background: {
        default: '#F8FAFC',
        paper: '#ffffff',
      },
      text: {
        primary: '#0F172A',
        secondary: '#64748B',
      },
      divider: 'rgba(0,0,0,0.07)',
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
            border: '1px solid rgba(0,0,0,0.07)',
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          outlined: {
            borderColor: 'rgba(0,0,0,0.07)',
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
              fontSize: '0.70rem',
              textTransform: 'uppercase',
              letterSpacing: '0.06em',
              color: '#64748B',
              backgroundColor: '#F8FAFC',
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
            '&:hover': { backgroundColor: alpha('#4F46E5', 0.03) },
            '&:last-child td': { border: 0 },
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: {
            borderBottom: '1px solid rgba(0,0,0,0.05)',
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
      // O vão do contorno (notch) é calculado com a fonte do campo; com o rótulo maior que o texto
      // (1rem x 0,875rem) a borda cortava o fim do rótulo, como no asterisco de "Emissão *".
      MuiInputLabel: {
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
