package com.airbnb.mvrx.todomvrx.core

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.todomvrx.TasksState
import com.airbnb.mvrx.todomvrx.TasksViewModel
import com.airbnb.mvrx.todomvrx.data.Tasks
import com.airbnb.mvrx.todomvrx.data.findTask
import com.airbnb.mvrx.todomvrx.todoapp.R
import com.airbnb.mvrx.todomvrx.util.ToDoEpoxyController
import com.airbnb.mvrx.todomvrx.util.showLongSnackbar

abstract class BaseFragment : BaseMvRxFragment() {

    protected val viewModel by activityViewModel(TasksViewModel::class)

    protected lateinit var coordinatorLayout: CoordinatorLayout
    protected lateinit var recyclerView: EpoxyRecyclerView
    protected lateinit var fab: FloatingActionButton
    protected val epoxyController by lazy {epoxyController() }
    // Used to keep track of task changes to determine if we should show a snackbar.
    private var oldTasks: Tasks? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_base, container, false).apply {
                coordinatorLayout = findViewById(R.id.coordinator_layout)
                fab = findViewById(R.id.fab)
                recyclerView = findViewById(R.id.recycler_view)
                recyclerView.setController(epoxyController)
            }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.selectSubscribe(TasksState::tasks, TasksState::lastEditedTask) { tasks, lastEditedTask ->
            if (oldTasks == null) {
                oldTasks = tasks
                return@selectSubscribe
            }

            if (oldTasks?.any { it.complete } == true && !tasks.any { it.complete }) {
                coordinatorLayout.showLongSnackbar(R.string.completed_tasks_cleared)
            }

            val oldTask = oldTasks?.findTask(lastEditedTask)
            val newTask = tasks.findTask(lastEditedTask)
            if (oldTask == newTask) return@selectSubscribe
            val message = when {
                oldTask == null -> R.string.successfully_added_task_message
                newTask == null -> R.string.successfully_deleted_task_message
                oldTask.title != newTask.title || oldTask.description != newTask.description ->
                    R.string.successfully_saved_task_message
                oldTask.complete && !newTask.complete -> R.string.task_marked_active
                !oldTask.complete && newTask.complete -> R.string.task_marked_complete
                else -> 0
            }
            if (message != 0) {
                coordinatorLayout.showLongSnackbar(message)
            }
            oldTasks = tasks
        }

        viewModel.asyncSubscribe(TasksState::taskRequest, onFail = {
            coordinatorLayout.showLongSnackbar(R.string.loading_tasks_error)
        })
    }

    override fun invalidate() {
        recyclerView.requestModelBuild()
    }

    abstract fun epoxyController(): ToDoEpoxyController

    protected fun navigate(@IdRes id: Int, args: Parcelable? = null) {
        findNavController().navigate(id, Bundle().apply { putParcelable(MvRx.KEY_ARG, args) })
    }
}