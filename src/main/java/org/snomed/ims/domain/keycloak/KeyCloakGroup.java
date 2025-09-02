package org.snomed.ims.domain.keycloak;

import java.io.Serializable;

/**
 * Group response from Keycloak.
 */
public class KeyCloakGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String parentId;

    private String path;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
