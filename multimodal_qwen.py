from transformers import BlipProcessor, BlipForConditionalGeneration
from PIL import Image
import subprocess

# 1. Load image and describe it using Qwen-VL
image_path = "example.jpg"  # 🔁 change this to your image path
prompt_text = "Describe this image."

print("[🖼] Loading vision model...")
processor = BlipProcessor.from_pretrained("Salesforce/blip-image-captioning-base")
model = BlipForConditionalGeneration.from_pretrained("Salesforce/blip-image-captioning-base")

image = Image.open(image_path).convert("RGB")

print("[🧠] Generating image caption...")
inputs = processor(images=image, text=prompt_text, return_tensors="pt")
generated_ids = model.generate(**inputs, max_new_tokens=128)
caption = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]

print(f"\n[✅] Image description: {caption}")

# 2. Build conversation-style prompt for LLM
llm_prompt = f"""
<|im_start|>system
You are a helpful assistant.<|im_end|>
<|im_start|>user
An image was described as: "{caption}"
Now answer this: What could be happening in the scene, and how does it relate to human emotion?<|im_end|>
"""

# 3. Send prompt to local LLM (Qwen2.5-Omni-3B via llama.cpp)
print("\n[🤖] Running local LLM on image description...\n")

llama_cmd = [
    "D:\\DFiles\\llama.cpp\\build\\bin\\Release\\llama-cli.exe",
    "-m", "D:\\DFiles\\llama.cpp\\models\\qwen\\qwen.Q4_K_M.gguf",
    "-p", llm_prompt,
    "--ctx-size", "2048",
    "--threads", "6"
]

process = subprocess.Popen(
    llama_cmd,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True
)
print("[🧠] LLM Response:\n")

for line in process.stdout:
    print(line, end="")

process.wait()

