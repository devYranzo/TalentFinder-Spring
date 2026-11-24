#!/usr/bin/env bash
# Descarga el modelo de embeddings, fijado a una revisión concreta, y comprueba su integridad.
# Uso (desde cualquier carpeta):  bash scripts/fetch-models.sh
set -euo pipefail

cd "$(dirname "$0")/.."

MODEL_DIR="models/multilingual-e5-small"
# Revisión fija del repositorio (commit que añadió el ONNX). No se usa "main": podría cambiar.
REVISION="ffdcc22a9a5c973ef0470385cef91e1ecb461d9f"
BASE_URL="https://huggingface.co/intfloat/multilingual-e5-small/resolve/${REVISION}/onnx"
# SHA-256 publicado por Hugging Face para onnx/tokenizer.json (puntero LFS).
TOKENIZER_SHA256="0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39"

command -v curl >/dev/null 2>&1 || { echo "Falta curl."; exit 1; }
command -v sha256sum >/dev/null 2>&1 || { echo "Falta sha256sum."; exit 1; }

mkdir -p "$MODEL_DIR"

for file in model.onnx tokenizer.json; do
  if [ -s "$MODEL_DIR/$file" ]; then
    echo "Ya existe $MODEL_DIR/$file"
  else
    echo "Descargando $file (puede tardar: model.onnx pesa unos 470 MB)..."
    curl -fL --retry 3 -o "$MODEL_DIR/$file.part" "$BASE_URL/$file"
    mv "$MODEL_DIR/$file.part" "$MODEL_DIR/$file"
  fi
done

actual="$(sha256sum "$MODEL_DIR/tokenizer.json" | cut -d' ' -f1)"
if [ "$actual" != "$TOKENIZER_SHA256" ]; then
  echo "ERROR: el hash de tokenizer.json no coincide con el publicado."
  echo "  esperado: $TOKENIZER_SHA256"
  echo "  obtenido: $actual"
  echo "Borra $MODEL_DIR y vuelve a intentarlo. Si se repite, no uses estos archivos."
  exit 1
fi

# Para model.onnx no tengo un hash publicado que comprobar: la primera vez se registra el que
# has descargado (confianza en el primer uso) y desde entonces se verifica contra ese registro.
if [ -f models/SHA256SUMS ]; then
  (cd models && sha256sum -c SHA256SUMS)
else
  (cd models && sha256sum multilingual-e5-small/model.onnx multilingual-e5-small/tokenizer.json > SHA256SUMS)
  echo "Primera descarga: hashes registrados en models/SHA256SUMS."
  echo "Haz commit de ese archivo para que cualquier otra máquina verifique los mismos hashes."
fi

echo "Modelo listo en $MODEL_DIR"