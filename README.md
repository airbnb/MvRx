# MvRx: Android on Autopilot

## For full documentation, check out the [wiki](https://github.com/airbnb/MvRx/wiki)

MvRx (pronounced mavericks) is the Android framework from Airbnb that we use for nearly all product development at Airbnb.

When we began creating MvRx, our goal was not to create yet another architecture pattern for Airbnb, it was to make building products easier, faster, and more fun. All of our decisions have built on that. We believe that for MvRx to be successful, it must be effective for building everything from the simplest of screens to the most complex in our app.

This is what it looks like:
```kotlin

data class HelloWorldState(val title: String = "Hello World") : MvRxState

class HelloWorldViewModel(initialState: HelloWorldState) : MvRxViewModel<HelloWorldState>(initialState) {
    fun getMoreExcited() = setState { copy(title = "$title!") }
}

class HelloWorldFragment : BaseFragment() {
    private val viewModel by fragmentViewModel(HelloWorldViewModel::class)

    override fun EpoxyController.buildModels() = withRenderingState(viewModel) { state ->
        header {
            title(state.title)
        }
        basicRow { 
            onClick { viewModel.getMoreExcited() }
        }
    }
}
```

## For full documentation, check out the [wiki](https://github.com/airbnb/MvRx/wiki)
