function M=CBOX_mask_refinement(X,M)

[Nrow,Ncol,Nban]=size(X); %Image size after removing blank pixels 

%Mask of invalid pixels is available only for v4.1. 
%If the mask is not available we create an empty mask of the same size than the image
if ~exist('M'), M=zeros(Nrow,Ncol,Nban); end

%Although we have the mask with the drop-outs (Channel2 reset), 
%in some images there are invalid pixels that are not masked.
%The problem with these pixels is that do not present values <=0 
%(their values are in the order of magnitude of the signal) 
%but they always occur in odd columns.

%Therefore, we need to find rows with drop-out for both cases:
% - to create the mask for versions previous to v4.1
% - to improve the mask adding undetected drop-outs

%To find these rows we assume that the difference between contiguous pixels in a row is small. 
%The difference between a correct pixel with its neighbour should be smaller than with the pixel of two columns away, 
%except in the case that the neighbour is a drop-out. And this assumption should be true for all the odd pixels of the row.

for i=1:Nban %for each band
  for j=1:Nrow  %for each row
    %Square difference of odd pixels (possible dorputs) with neighbouring even pixels (correct)
    Hf=(X(j,1:1:Ncol-1,i)-X(j,2:1:Ncol,i)).^2; %big differences if we have drop-outs
    %Square difference of even pixels (correct) with the following even pixel
    Lf=(X(j,2:2:Ncol-2,i)-X(j,4:2:Ncol,i)).^2; %small differences in all cases (depending on the surface changes)
    %Surface and vertical striping affect differences between contiguous columns,
    %thus the 'median' is used to obtain an estimation for the whole row avoiding outliers
    %(more robust estimators can be used if needed)
    DOtest=median(Hf)/median(Lf); 
    if  DOtest > 1.5     %If difference between neighbours is higher than between even pixels 
      M(j,1:2:Ncol,i)=1; %we label all odd pixels as drop-out noise
    end
  end
end

return


