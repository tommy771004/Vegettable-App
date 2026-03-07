import SwiftUI

struct SkeletonView: View {
    @State private var shimmerPhase: CGFloat = -1.0

    var body: some View {
        GeometryReader { geo in
            Color.gray.opacity(0.15)
                .overlay(
                    LinearGradient(
                        gradient: Gradient(stops: [
                            .init(color: .clear,                          location: 0.0),
                            .init(color: .white.opacity(0.5),             location: 0.4),
                            .init(color: .clear,                          location: 0.8)
                        ]),
                        startPoint: .init(x: shimmerPhase - 0.3, y: 0),
                        endPoint:   .init(x: shimmerPhase,        y: 0)
                    )
                )
        }
        .cornerRadius(8)
        .onAppear {
            withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                shimmerPhase = 1.3
            }
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
