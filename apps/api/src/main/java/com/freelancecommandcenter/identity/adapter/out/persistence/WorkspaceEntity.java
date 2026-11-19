package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.domain.Workspace;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "identity", name = "workspaces")
class WorkspaceEntity {

    @Id
    private UUID id;

    private UUID ownerUserId;
    private Instant createdAt;

    protected WorkspaceEntity() {}

    WorkspaceEntity(Workspace workspace) {
        id = workspace.id().value();
        ownerUserId = workspace.ownerId().value();
        createdAt = workspace.createdAt();
    }

    Workspace toDomain() {
        return new Workspace(new WorkspaceId(id), new UserId(ownerUserId), createdAt);
    }
}
