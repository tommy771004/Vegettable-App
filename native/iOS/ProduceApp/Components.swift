import SwiftUI

struct SkeletonView: View {
    @State private var blink = false
    
    var body: some View {
        Color.gray.opacity(0.3)
            .cornerRadius(8)
            .opacity(blink ? 0.3 : 1.0)
            .animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true), value: blink)
            .onAppear {
                blink.toggle()
            }
    }
}

struct ErrorView: View {
    let message: String
    let retryAction: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 50))
                .foregroundColor(.red)
            
            Text(message)
                .font(.headline)
                .multilineTextAlignment(.center)
            
            Button(action: retryAction) {
                Text("重試")
                    .fontWeight(.bold)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
        }
        .padding()
    }
}

struct EmptyStateView: View {
    let message: String
    let systemImage: String
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: systemImage)
                .font(.system(size: 60))
                .foregroundColor(.gray)
            
            Text(message)
                .font(.title3)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}
