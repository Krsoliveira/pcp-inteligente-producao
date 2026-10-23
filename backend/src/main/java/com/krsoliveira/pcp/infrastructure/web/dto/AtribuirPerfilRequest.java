package com.krsoliveira.pcp.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AtribuirPerfilRequest(@NotNull(message = "Perfil é obrigatório") UUID perfilId) {}
