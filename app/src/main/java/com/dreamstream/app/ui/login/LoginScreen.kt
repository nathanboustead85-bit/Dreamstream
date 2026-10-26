@Composable
fun LoginScreen() {

    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column {

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Server URL") }
        )

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Username") }
        )

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Password") }
        )

        Button(onClick = { }) {
            Text("CONNECT")
        }
    }
}
