package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import com.freelancecommandcenter.identity.application.port.out.WorkspaceRepository;
import com.freelancecommandcenter.identity.domain.Email;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.identity.domain.Workspace;
import com.freelancecommandcenter.shared.id.UserId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stores users and workspaces. Updates load the entity first so optimistic locking stays effective. */
@Component
class AccountPersistenceAdapter implements UserRepository, WorkspaceRepository {

    private final UserJpaRepository users;
    private final WorkspaceJpaRepository workspaces;

    AccountPersistenceAdapter(UserJpaRepository users, WorkspaceJpaRepository workspaces) {
        this.users = users;
        this.workspaces = workspaces;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return users.findById(id.value()).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.findByEmail(email.value()).map(UserEntity::toDomain);
    }

    @Override
    public void save(User user) {
        UUID id = user.id().value();
        UserEntity entity = users.findById(id).orElseGet(() -> new UserEntity(id));
        entity.apply(user);
        users.save(entity);
    }

    @Override
    public Optional<Workspace> findByOwner(UserId ownerId) {
        return workspaces.findByOwnerUserId(ownerId.value()).map(WorkspaceEntity::toDomain);
    }

    @Override
    public void save(Workspace workspace) {
        workspaces.save(new WorkspaceEntity(workspace));
    }
}
