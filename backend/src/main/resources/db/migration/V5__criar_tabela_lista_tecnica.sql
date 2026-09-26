-- V5: Tabelas de lista técnica / BOM (Fase 5a)
--
-- lista_tecnica: BOM versionada por material. Apenas uma versão pode estar ATIVA
-- por material por vez (garantido pela aplicação; a constraint UNIQUE em
-- (material_id, versao) garante unicidade de versão).
--
-- item_lista_tecnica: componentes da BOM. Suporta BOM multinível — um componente
-- pode ser um semiacabado que possui sua própria lista técnica.

CREATE TABLE lista_tecnica (
    id          UUID         NOT NULL,
    material_id UUID         NOT NULL,
    versao      VARCHAR(50)  NOT NULL,
    status      VARCHAR(15)  NOT NULL,
    criada_em   TIMESTAMPTZ  NOT NULL,
    atualizada_em TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_lista_tecnica PRIMARY KEY (id),
    CONSTRAINT fk_lista_tecnica_material FOREIGN KEY (material_id) REFERENCES material (id),
    CONSTRAINT uq_lista_tecnica_material_versao UNIQUE (material_id, versao),
    CONSTRAINT ck_lista_tecnica_status CHECK (status IN ('EM_REVISAO', 'ATIVA', 'OBSOLETA'))
);

CREATE INDEX idx_lista_tecnica_material ON lista_tecnica (material_id);
CREATE INDEX idx_lista_tecnica_status   ON lista_tecnica (status);

COMMENT ON TABLE lista_tecnica IS 'Lista Técnica (BOM) versionada. Define os componentes para produzir uma unidade do material-pai.';
COMMENT ON COLUMN lista_tecnica.versao IS 'Identificador de versão livre (ex: v1, v2-tolerancia-extra).';

-- ---------------------------------------------------------------------------

CREATE TABLE item_lista_tecnica (
    id                    UUID           NOT NULL,
    lista_tecnica_id      UUID           NOT NULL,
    material_componente_id UUID          NOT NULL,
    quantidade_planejada  NUMERIC(14, 4) NOT NULL,
    unidade_de_medida     VARCHAR(10)    NOT NULL,

    CONSTRAINT pk_item_lista_tecnica PRIMARY KEY (id),
    CONSTRAINT fk_item_lista_tecnica_lista FOREIGN KEY (lista_tecnica_id)
        REFERENCES lista_tecnica (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_lista_tecnica_material FOREIGN KEY (material_componente_id)
        REFERENCES material (id),
    CONSTRAINT ck_item_quantidade CHECK (quantidade_planejada > 0)
);

CREATE INDEX idx_item_lista_tecnica_lista ON item_lista_tecnica (lista_tecnica_id);

COMMENT ON TABLE item_lista_tecnica IS 'Componentes da lista técnica (BOM). ON DELETE CASCADE: ao remover uma lista, seus itens são removidos junto.';
COMMENT ON COLUMN item_lista_tecnica.material_componente_id IS 'Referência ao material componente (pode ser MP, SA ou PA).';