import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import Box from '@mui/material/Box'
import Drawer from '@mui/material/Drawer'
import AppBar from '@mui/material/AppBar'
import Toolbar from '@mui/material/Toolbar'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import ListSubheader from '@mui/material/ListSubheader'
import Typography from '@mui/material/Typography'
import IconButton from '@mui/material/IconButton'
import Divider from '@mui/material/Divider'
import Tooltip from '@mui/material/Tooltip'
import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import FactoryIcon from '@mui/icons-material/Factory'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '../store/authStore'
import { usePermissoes } from '../hooks/usePermissoes'
import { secoesVisiveis, type NavItem } from './navegacao'

const DRAWER_WIDTH = 248

// Sidebar usa fundo escuro (#0F172A) — padrão de enterprise dashboards.
const SIDEBAR_BG = '#0F172A'
const SIDEBAR_TEXT = 'rgba(255,255,255,0.65)'
const SIDEBAR_SUBHEADER = 'rgba(255,255,255,0.35)'
const SIDEBAR_DIVIDER = 'rgba(255,255,255,0.08)'
const SIDEBAR_ACTIVE_BG = '#4F46E5'
const SIDEBAR_HOVER_BG = 'rgba(255,255,255,0.06)'

export function Layout() {
  const navigate = useNavigate()
  const location = useLocation()
  const logout = useAuthStore((s) => s.logout)
  const queryClient = useQueryClient()
  const { usuario, temAlguma } = usePermissoes()
  const secoes = secoesVisiveis(temAlguma)
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleLogout = () => {
    logout()
    // Nada da sessão anterior (inclusive as permissões) pode aparecer para o próximo usuário.
    queryClient.clear()
    navigate('/login')
  }

  const isAtivo = ({ rota, ativoTambem = [] }: NavItem) =>
    rota === '/dashboard'
      ? location.pathname === rota
      : [rota, ...ativoTambem].some((r) => location.pathname.startsWith(r))

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: SIDEBAR_BG }}>
      {/* Logo */}
      <Box sx={{ px: 2.5, py: 2.5, display: 'flex', alignItems: 'center', gap: 1.25 }}>
        <Box
          sx={{
            width: 34,
            height: 34,
            borderRadius: 2,
            bgcolor: SIDEBAR_ACTIVE_BG,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <FactoryIcon sx={{ color: 'white', fontSize: 20 }} />
        </Box>
        <Box>
          <Typography
            variant="subtitle2"
            sx={{ lineHeight: 1.2, color: 'white', fontSize: '0.85rem', fontWeight: 600 }}
          >
            PCP Inteligente
          </Typography>
          <Typography sx={{ fontSize: '0.68rem', color: SIDEBAR_SUBHEADER }}>
            Plataforma de Produção
          </Typography>
        </Box>
      </Box>

      <Divider sx={{ borderColor: SIDEBAR_DIVIDER }} />

      {/* Seções de navegação */}
      <Box sx={{ flexGrow: 1, overflow: 'auto', py: 1 }}>
        {secoes.map((section, sIdx) => (
          <Box key={section.titulo}>
            {sIdx > 0 && <Divider sx={{ my: 0.5, mx: 2, borderColor: SIDEBAR_DIVIDER }} />}
            <List
              dense
              subheader={
                <ListSubheader
                  disableSticky
                  sx={{
                    bgcolor: 'transparent',
                    color: SIDEBAR_SUBHEADER,
                    fontSize: '0.62rem',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    letterSpacing: '0.08em',
                    lineHeight: '2rem',
                    px: 2,
                  }}
                >
                  {section.titulo}
                </ListSubheader>
              }
              sx={{ px: 1, pb: 0 }}
            >
              {section.itens.map((item) => {
                const ativo = isAtivo(item)
                return (
                  <ListItemButton
                    key={item.rota}
                    selected={ativo}
                    onClick={() => {
                      navigate(item.rota)
                      setMobileOpen(false)
                    }}
                    sx={{
                      borderRadius: 1.5,
                      mb: 0.25,
                      px: 1.5,
                      py: 0.75,
                      '&.Mui-selected': {
                        bgcolor: SIDEBAR_ACTIVE_BG,
                        color: 'white',
                        '& .MuiListItemIcon-root': { color: 'white' },
                        '&:hover': { bgcolor: SIDEBAR_ACTIVE_BG },
                      },
                      '&:not(.Mui-selected)': {
                        color: SIDEBAR_TEXT,
                        '& .MuiListItemIcon-root': { color: SIDEBAR_TEXT },
                      },
                      '&:not(.Mui-selected):hover': {
                        bgcolor: SIDEBAR_HOVER_BG,
                        color: 'white',
                        '& .MuiListItemIcon-root': { color: 'white' },
                      },
                    }}
                  >
                    <ListItemIcon sx={{ minWidth: 32 }}>
                      {item.icon}
                    </ListItemIcon>
                    <ListItemText
                      primary={item.label}
                      primaryTypographyProps={{
                        fontSize: '0.825rem',
                        fontWeight: ativo ? 600 : 400,
                      }}
                    />
                  </ListItemButton>
                )
              })}
            </List>
          </Box>
        ))}
      </Box>

      {/* Quem está logado e com qual perfil */}
      {usuario && (
        <>
          <Divider sx={{ borderColor: SIDEBAR_DIVIDER }} />
          <Box sx={{ px: 2.5, py: 1.25 }}>
            <Typography noWrap sx={{ fontSize: '0.78rem', color: 'white', fontWeight: 600 }}>
              {usuario.nome}
            </Typography>
            <Typography noWrap sx={{ fontSize: '0.68rem', color: SIDEBAR_SUBHEADER }}>
              {usuario.perfil.nome}
            </Typography>
          </Box>
        </>
      )}

      {/* Logout */}
      <Divider sx={{ borderColor: SIDEBAR_DIVIDER }} />
      <List sx={{ px: 1, py: 0.5 }}>
        <ListItemButton
          onClick={handleLogout}
          sx={{
            borderRadius: 1.5,
            px: 1.5,
            py: 0.75,
            color: SIDEBAR_TEXT,
            '& .MuiListItemIcon-root': { color: SIDEBAR_TEXT },
            '&:hover': {
              bgcolor: SIDEBAR_HOVER_BG,
              color: 'white',
              '& .MuiListItemIcon-root': { color: 'white' },
            },
          }}
        >
          <ListItemIcon sx={{ minWidth: 32 }}>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText
            primary="Sair"
            primaryTypographyProps={{ fontSize: '0.825rem' }}
          />
        </ListItemButton>
      </List>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex' }}>
      {/* AppBar (mobile) — mantém fundo claro para contraste com o conteúdo */}
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          display: { sm: 'none' },
          bgcolor: 'background.paper',
          color: 'text.primary',
          borderBottom: '1px solid',
          borderColor: 'divider',
          zIndex: (t) => t.zIndex.drawer + 1,
        }}
      >
        <Toolbar sx={{ minHeight: '56px !important' }}>
          <IconButton edge="start" onClick={() => setMobileOpen(!mobileOpen)} sx={{ mr: 1.5 }}>
            <MenuIcon />
          </IconButton>
          <Box
            sx={{
              width: 28,
              height: 28,
              borderRadius: 1.5,
              bgcolor: 'primary.main',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              mr: 1,
            }}
          >
            <FactoryIcon sx={{ color: 'white', fontSize: 16 }} />
          </Box>
          <Typography variant="subtitle2" fontWeight={700}>
            PCP Inteligente
          </Typography>
          <Box sx={{ flexGrow: 1 }} />
          <Tooltip title="Sair">
            <IconButton onClick={handleLogout} size="small" color="inherit">
              <LogoutIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      {/* Drawer mobile */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            bgcolor: SIDEBAR_BG,
            borderRight: 'none',
          },
        }}
      >
        {drawerContent}
      </Drawer>

      {/* Drawer desktop */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', sm: 'block' },
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            bgcolor: SIDEBAR_BG,
            borderRight: 'none',
          },
        }}
        open
      >
        {drawerContent}
      </Drawer>

      {/* Conteúdo */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          minWidth: 0,
          minHeight: '100vh',
          bgcolor: 'background.default',
          mt: { xs: '56px', sm: 0 },
          p: { xs: 2, sm: 3 },
          ml: { sm: `${DRAWER_WIDTH}px` },
          maxWidth: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
        }}
      >
        <Outlet />
      </Box>
    </Box>
  )
}
