#!/bin/bash

# Configuration from environment variables
OLLAMA_MODEL=${OLLAMA_MODEL:-phi3}
OLLAMA_AUTO_PULL=${OLLAMA_AUTO_PULL:-true}
OLLAMA_STARTUP_TIMEOUT=${OLLAMA_STARTUP_TIMEOUT:-30}

echo "🚀 Starting Ollama with automatic $OLLAMA_MODEL setup..."
echo "📋 Configuration:"
echo "   Model: $OLLAMA_MODEL"
echo "   Auto-pull: $OLLAMA_AUTO_PULL"
echo "   Timeout: ${OLLAMA_STARTUP_TIMEOUT}s"

# Start Ollama in the background
ollama serve &
OLLAMA_PID=$!

# Function to cleanup on exit
cleanup() {
    echo "🛑 Shutting down Ollama..."
    kill $OLLAMA_PID 2>/dev/null
    wait $OLLAMA_PID 2>/dev/null
    exit 0
}

# Setup signal handlers
trap cleanup SIGTERM SIGINT

# Wait for Ollama to be ready
echo "⏳ Waiting for Ollama to be ready..."
max_attempts=$OLLAMA_STARTUP_TIMEOUT
attempt=0

while [ $attempt -lt $max_attempts ]; do
    # Use ollama list command to check if service is ready
    if ollama list >/dev/null 2>&1; then
        echo "✅ Ollama is ready!"
        break
    fi
    attempt=$((attempt + 1))
    echo "   Attempt $attempt/$max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Ollama failed to start after $((max_attempts * 2)) seconds"
    echo "🔍 Checking if Ollama process is running..."
    if kill -0 $OLLAMA_PID 2>/dev/null; then
        echo "✅ Ollama process is running, continuing anyway..."
    else
        echo "❌ Ollama process has died"
        exit 1
    fi
fi

# Handle model download/verification
if [ "$OLLAMA_AUTO_PULL" = "true" ]; then
    echo "🔍 Checking for $OLLAMA_MODEL model..."
    if ollama list | grep -q "$OLLAMA_MODEL"; then
        echo "✅ $OLLAMA_MODEL model already exists"
    else
        echo "📥 Downloading $OLLAMA_MODEL model..."
        echo "   This may take several minutes depending on model size..."
        
        # Get model info for better user experience
        case $OLLAMA_MODEL in
            "phi3")
                echo "   📦 phi3: 2.3GB - Fast, efficient, good for development"
                ;;
            "llama3")
                echo "   📦 llama3: 4.7GB - High quality, best for production"
                ;;
            "codellama")
                echo "   📦 codellama: 3.8GB - Optimized for code analysis"
                ;;
            "mistral")
                echo "   📦 mistral: 4.1GB - Balanced performance and quality"
                ;;
            "llama3.1")
                echo "   📦 llama3.1: 4.7GB - Latest version with improvements"
                ;;
            *)
                echo "   📦 $OLLAMA_MODEL: Unknown size - Custom model"
                ;;
        esac
        
        if ollama pull $OLLAMA_MODEL; then
            echo "✅ $OLLAMA_MODEL model downloaded successfully!"
        else
            echo "❌ Failed to download $OLLAMA_MODEL model"
            echo "💡 You can manually download it later with: docker exec ats-ollama ollama pull $OLLAMA_MODEL"
            echo "💡 Or disable auto-pull with OLLAMA_AUTO_PULL=false"
        fi
    fi
else
    echo "⏭️  Auto-pull disabled, skipping model download"
    echo "💡 Make sure to manually pull required models"
fi

echo "🎉 Ollama setup complete!"
echo "📋 Available models:"
ollama list

# Keep the script running and forward signals to Ollama
wait $OLLAMA_PID 