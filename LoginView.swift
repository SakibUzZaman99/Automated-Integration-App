//
//  LoginView.swift
//  MehuAPP
//
//  Created by MacBook Air on 22/6/25.
//

import SwiftUI
import FirebaseAuth
import GoogleSignIn
import GoogleSignInSwift

struct LoginView: View {
    @AppStorage("isLoggedIn") var isLoggedIn = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 40) {
            Text("Welcome to ZapierMini")
                .font(.largeTitle)
                .fontWeight(.bold)

            GoogleSignInButton {
                signInWithGoogle()
            }
            .frame(height: 48)
            .padding()

            if let error = errorMessage {
                Text(error)
                    .foregroundColor(.red)
            }
        }
        .padding()
    }

    func signInWithGoogle() {
        guard let rootVC = UIApplication.shared.rootController else {
            self.errorMessage = "Could not find root view controller"
            return
        }

        Task {
            do {
                let signInResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootVC)
                guard let idToken = signInResult.user.idToken?.tokenString else {
                    throw NSError(domain: "GoogleSignIn", code: -1, userInfo: [NSLocalizedDescriptionKey: "Missing ID token"])
                }

                let credential = GoogleAuthProvider.credential(
                    withIDToken: idToken,
                    accessToken: signInResult.user.accessToken.tokenString
                )

                try await Auth.auth().signIn(with: credential)
                isLoggedIn = true

            } catch {
                self.errorMessage = error.localizedDescription
            }
        }
    }

}
