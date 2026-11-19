package com.freelancecommandcenter.identity.application.port.out;

import com.freelancecommandcenter.identity.domain.Workspace;
import com.freelancecommandcenter.shared.id.UserId;
import java.util.Optional;

public interface WorkspaceRepository {

    Optional<Workspace> findByOwner(UserId ownerId);

    void save(Workspace workspace);
}
