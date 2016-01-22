/*
 * Copyright 2013 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.calculator.math.edit;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import org.solovyev.android.Check;
import org.solovyev.android.calculator.BaseFragment;
import org.solovyev.android.calculator.CalculatorFragmentType;
import org.solovyev.android.calculator.R;
import org.solovyev.android.views.llm.DividerItemDecoration;
import org.solovyev.common.math.MathEntity;
import org.solovyev.common.text.Strings;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import butterknife.Bind;
import butterknife.ButterKnife;


public abstract class BaseEntitiesFragment<E extends MathEntity> extends BaseFragment {

    public static final String EXTRA_CATEGORY = "category";
    private static final Comparator<MathEntity> COMPARATOR = new Comparator<MathEntity>() {
        @Override
        public int compare(MathEntity l, MathEntity r) {
            return l.getName().compareTo(r.getName());
        }
    };

    @Nonnull
    private final Handler uiHandler = new Handler();
    @Bind(R.id.entities_fab)
    FloatingActionButton fab;
    @Bind(R.id.entities_recyclerview)
    RecyclerView recyclerView;
    private EntitiesAdapter adapter;
    @Nullable
    private String category;

    protected BaseEntitiesFragment(@Nonnull CalculatorFragmentType type) {
        super(type);
    }

    @Nonnull
    public static Bundle createBundleFor(@Nonnull String categoryId) {
        final Bundle result = new Bundle(1);
        putCategory(result, categoryId);
        return result;
    }

    static void putCategory(@Nonnull Bundle bundle, @Nonnull String categoryId) {
        bundle.putString(EXTRA_CATEGORY, categoryId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle != null) {
            category = bundle.getString(EXTRA_CATEGORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = ui.onCreateView(this, inflater, container);
        ButterKnife.bind(this, view);
        final Context context = inflater.getContext();
        adapter = new EntitiesAdapter(context, TextUtils.isEmpty(category) ? getEntities() : getEntities(category));
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(context, null));
        return view;
    }

    protected abstract void onClick(@Nonnull E entity);

    @Nonnull
    private List<E> getEntities(@NonNull String category) {
        Check.isNotEmpty(category);
        final List<E> entities = getEntities();

        final Iterator<E> iterator = entities.iterator();
        while (iterator.hasNext()) {
            final E entity = iterator.next();
            if (!isInCategory(entity, category)) {
                iterator.remove();
            }
        }

        return entities;
    }

    protected final boolean isInCategory(@NonNull E entity) {
        return TextUtils.isEmpty(category) || isInCategory(entity, category);
    }

    private boolean isInCategory(@NonNull E entity, @NonNull String category) {
        return TextUtils.equals(getCategory(entity), category);
    }

    @Nonnull
    protected abstract List<E> getEntities();

    @Nullable
    abstract String getCategory(@Nonnull E e);

    protected EntitiesAdapter getAdapter() {
        return adapter;
    }

    @Nonnull
    protected Handler getUiHandler() {
        return uiHandler;
    }

    @SuppressWarnings("deprecation")
    protected final void copyDescription(@Nonnull E entity) {
        final String description = getDescription(entity);
        if (!Strings.isEmpty(description)) {
            final ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
            clipboard.setText(description);
        }
    }

    @Nullable
    protected abstract String getDescription(@NonNull E entity);

    protected abstract void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull E entity, @Nonnull MenuItem.OnMenuItemClickListener listener);

    protected abstract boolean onMenuItemClicked(@Nonnull MenuItem item, @Nonnull E entity);

    public class EntityViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        @Bind(R.id.entity_text)
        TextView textView;
        @Bind(R.id.entity_description)
        TextView descriptionView;
        @Nullable
        private E entity;

        public EntityViewHolder(@Nonnull View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
            view.setOnCreateContextMenuListener(this);
        }

        public void bind(@Nonnull E entity) {
            this.entity = entity;
            textView.setText(String.valueOf(entity));

            final String description = getDescription(entity);
            if (!Strings.isEmpty(description)) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(description);
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            Check.isNotNull(entity);
            BaseEntitiesFragment.this.onClick(entity);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            Check.isNotNull(entity);
            BaseEntitiesFragment.this.onCreateContextMenu(menu, entity, this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Check.isNotNull(entity);
            return BaseEntitiesFragment.this.onMenuItemClicked(item, entity);
        }
    }

    public class EntitiesAdapter extends RecyclerView.Adapter<EntityViewHolder> {
        @Nonnull
        private final LayoutInflater inflater;
        @Nonnull
        private final List<E> list;

        private EntitiesAdapter(@Nonnull Context context,
                                @Nonnull List<E> list) {
            this.list = list;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public EntityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new EntityViewHolder(inflater.inflate(R.layout.fragment_entities_item, parent, false));
        }

        @Override
        public void onBindViewHolder(EntityViewHolder holder, int position) {
            holder.bind(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Nonnull
        public E getItem(int position) {
            return list.get(position);
        }

        public void set(int position, @Nonnull E function) {
            list.set(position, function);
        }

        public void sort() {
            Collections.sort(list, COMPARATOR);
            notifyDataSetChanged();
        }

        public void add(@Nonnull E entity) {
            list.add(entity);
        }

        public void remove(@Nonnull E function) {
            list.remove(function);
        }
    }
}