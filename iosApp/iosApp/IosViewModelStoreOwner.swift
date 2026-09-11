import Shared

/// El dueño del ciclo de vida de los ViewModel en iOS.
///
/// En Android, quien decide cuándo muere un ViewModel es el sistema. En iOS no
/// hay equivalente, así que hay que escribirlo: esta clase guarda el almacén y
/// lo vacía en `deinit`, que es lo que acaba llamando a `onCleared()` y cancela
/// el `viewModelScope`.
///
/// Es `ObservableObject` para poder usarla como `@StateObject` en la vista, que
/// es lo único que garantiza que SwiftUI la cree una sola vez y no en cada
/// recomposición.
final class IosViewModelStoreOwner: ObservableObject, ViewModelStoreOwner {

    let viewModelStore = ViewModelStore()

    deinit {
        viewModelStore.clear()
    }
}
