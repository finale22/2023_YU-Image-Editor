from flask import Flask, request, send_file
from werkzeug.utils import secure_filename
from flask_restx import Resource, Api, Namespace
import os

# 이미지 처리 함수를 여기에 가져와서 'process_image'를 실제 이미지 처리 함수로 대체합니다
from processing import *

app = Flask(__name__)
api = Api(app)

# 네임스페이스 생성
ns = Namespace('file_operations', description='파일 업로드 및 다운로드 작업')

UPLOAD_FOLDER = 'C:/Process/Processed_image'  # 실제 디렉토리 주소
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# 이미지 저장 디렉토리가 없으면 생성
if not os.path.exists(app.config['UPLOAD_FOLDER']):
    os.makedirs(app.config['UPLOAD_FOLDER'])

@api.route('/upload')
class FileUpload(Resource):
    def post(self):
        # 이미지 처리 함수 호출
        # 받아온 이미지: request.files['image']
        processed_image = image_processing(request.files['image'])
        
        
        # 처리된 이미지 저장
        processed_image_path = os.path.join(app.config['UPLOAD_FOLDER'], 'processed_image.jpg')
        processed_image.save(processed_image_path)
        return {"isUploadSuccess": "success", "processed_image_path": processed_image_path}

@api.route('/download/<filename>')
class FileDownload(Resource):
    def get(self, filename):
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        if os.path.exists(file_path):
            return send_file(file_path)
        else:
            return {"message": "파일을 찾을 수 없습니다"}, 404

if __name__ == "__main__":
    # 네임스페이스를 API에 추가
    api.add_namespace(ns)
    app.run(debug=True, host='165.229.125.141', port=5000)
