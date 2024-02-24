import PIL
import numpy as np
import torch
from matplotlib import pyplot as plt
from torchvision import transforms

from builds import *
from models import *
from model_structures import *
from utils import *

def image_processing(image):
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    net_G = build_res_unet(n_input=1, n_output=2, size=256)
    net_G.load_state_dict(torch.load("C:/Users/user/Desktop/server/gray_to_color/unet/res18-unet.pt", map_location=device))
    model = MainModel(net_G=net_G)
    model.load_state_dict(
        torch.load(
            "C:/Users/user/Desktop/server/gray_to_color/weights/final_model_weights.pt",
            map_location=device
        )
    )
    img = PIL.Image.open(image)
    img = img.resize((256, 256))
    img = transforms.ToTensor()(img)[:1] * 2. - 1.
    model.eval()
    with torch.no_grad():
        preds = model.net_G(img.unsqueeze(0).to(device))
    colorized = lab_to_rgb(img.unsqueeze(0), preds.cpu())[0]
    colorized_img = PIL.Image.fromarray((colorized * 255).astype(np.uint8)).resize((256, 256)).convert('RGB')
    return colorized_img







    
