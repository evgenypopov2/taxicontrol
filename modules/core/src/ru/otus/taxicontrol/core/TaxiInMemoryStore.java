package ru.otus.taxicontrol.core;

import com.haulmont.cuba.core.app.DataStore;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.ValueLoadContext;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Component(TaxiInMemoryStore.NAME)
public class TaxiInMemoryStore implements DataStore {
    public static final String NAME = "taxicontrol_TaxiInMemoryStore";

    @Nullable
    @Override
    public <E extends Entity> E load(LoadContext<E> context) {
        return null;
    }

    @Override
    public <E extends Entity> List<E> loadList(LoadContext<E> context) {
        return null;
    }

    @Override
    public long getCount(LoadContext<? extends Entity> context) {
        return 0;
    }

    @Override
    public Set<Entity> commit(CommitContext context) {
        return null;
    }

    @Override
    public List<KeyValueEntity> loadValues(ValueLoadContext context) {
        return null;
    }
}