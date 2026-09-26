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
import DashboardIcon from '@mui/icons-material/Dashboard'
import ListAltIcon from '@mui/icons-material/ListAlt'
import InventoryIcon from '@mui/icons-material/Inventory2Outlined'
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined'
import AllInboxIcon from '@mui/icons-material/AllInboxOutlined'
import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import FactoryIcon from '@mui/icons-material/Factory'
import { useAuthStore } from '../store/authStore'

const DRAWER_WIDTH = 248

interface NavItem {
  label: string
  icon: React.ReactNode
  rota: string
}

interface NavSection {
  titulo: string
  itens: NavItem[]
}

const NAV_SECTIONS: NavSection[] = [
  {
    titulo: 'Painel',
    itens: [
      { label: 'Dashboard', icon: <DashboardIcon fontSize="small" />, rota: '/dashboard' },
    ],
  },
  {
    titulo: 'Cadastros',
    itens: [
      { label: 'Materiais', icon: <InventoryIcon fontSize="small" />, rota: '/materiais' },
      { label: 'Listas Técnicas', icon: <AccountTreeIcon fontSize="small" />, rota: '/listas-tecnicas' },
      { label: 'Tipos de Ordem', icon: <LabelOutlinedIcon fontSize="small" />, rota: '/tipos-ordem' },
    ],
  },
  {
    titulo: 'Produção',
    itens: [
      { label: 'Ordens', icon: <ListAltIcon fontSize="small" />, rota: '/ordens' },
      { label: 'Lotes', icon: <AllInboxIcon fontSize="small" />, rota: '/lotes' },
    ],
  },
]

export function Layout() {
  const navigate = useNavigate()
  const location = useLocation()
  const logout = useAuthStore((s) => s.logout)
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const isAtivo = (rota: string) =>
    rota === '/dashboard'
      ? location.pathname === rota
      : location.pathname.startsWith(rota)

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Logo */}
      <Box sx={{ px: 2.5, py: 2.5, display: 'flex', alignItems: 'center', gap: 1.25 }}>
        <Box
          sx={{
            width: 34,
            height: 34,
            borderRadius: 2,
            bgcolor: 'primary.main',
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
            sx={{ lineHeight: 1.2, color: 'text.primary', fontSize: '0.85rem' }}
          >
            PCP Inteligente
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.68rem' }}>
            Plataforma de Produção
          </Typography>
        </Box>
      </Box>

      <Divider />

      {/* Seções de navegação */}
      <Box sx={{ flexGrow: 1, overflow: 'auto', py: 1 }}>
        {NAV_SECTIONS.map((section, sIdx) => (
          <Box key={section.titulo}>
            {sIdx > 0 && <Divider sx={{ my: 0.5, mx: 2, opacity: 0.5 }} />}
            <List
              dense
              subheader={
                <ListSubheader disableSticky>{section.titulo}</ListSubheader>
              }
              sx={{ px: 1, pb: 0 }}
            >
              {section.itens.map((item) => {
                const ativo = isAtivo(item.rota)
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
                        bgcolor: 'primary.main',
                        color: 'white',
                        '& .MuiListItemIcon-root': { color: 'white' },
                        '&:hover': { bgcolor: 'primary.dark' },
                      },
                      '&:not(.Mui-selected):hover': {
                        bgcolor: 'rgba(21,101,192,0.06)',
                      },
                    }}
                  >
                    <ListItemIcon sx={{ minWidth: 32, color: ativo ? 'white' : 'text.secondary' }}>
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

      {/* Logout */}
      <Divider />
      <List sx={{ px: 1, py: 0.5 }}>
        <ListItemButton onClick={handleLogout} sx={{ borderRadius: 1.5, px: 1.5, py: 0.75 }}>
          <ListItemIcon sx={{ minWidth: 32, color: 'text.secondary' }}>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText
            primary="Sair"
            primaryTypographyProps={{ fontSize: '0.825rem', color: 'text.secondary' }}
          />
        </ListItemButton>
      </List>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex' }}>
      {/* AppBar (mobile) */}
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
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
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
            borderRight: '1px solid',
            borderColor: 'divider',
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
