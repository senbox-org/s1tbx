function [X,M]=CBOX_dropout_correction(X,M,N,nB)
% Performs the 2D convolution of matrix X by a matrix of weights in the neighbourhood defined by N
%  'X' is a 3D matrix with the image
%  'M' is a 3D matrix with the same size of X with the masked values in X
%     Mask M=0 in the useful values and >0 in invalid values. 
%     ONLY pixels indicated by M=DOMV will be corrected (drop-out mask value, M=DOMV=1).
%     ONLY useful pixels M=0 will be used to correct the others.
%  'N' is a 2D 3x3 matrix that defines the used neighbours (value>0) in a 3x3 neighbourhood
%     Examples: Nvertical =[0 1 0;0 0 0;0 1 0]; Nhorizonal=[0 0 0;1 0 1;0 0 0];
%               N4conected=[0 1 0;1 0 1;0 1 0]; N8conected=[1 1 1;1 0 1;1 1 1];
%               Nuserdefined = [0.5 1 0.5;1 0 1;0.5 1 0.5];
%  'nB' is the number of upper and bottom bands used to compute the spectral distance in the correction process.
%
%  Each corrected pixel band is computed as the weighted average of the values of the neighbouring pixels defined in N.
%   - The weight of each neighbour is based on its spectral distance to the pixel which is being corrected.
%   - The spectral distance is computed locally using the 'nB' upper and 'nB' bottom bands.
%  The result is similar to a spatial interpolation but taking into account the similarity with neighbours.
%
%  When no useful pixels (M=0) are available in the defined neighbourhood, a WRONG value is returned (WRONG=NaN).

[Nrow,Ncol,Nban]=size(X); %sixe of the input image

DOMV=1;    %value of the drop-outs to be corrected in the mask M
CDOMV=1;   %value of the corrected drop-outs in the returned mask M
WRONG=NaN; %returned value for uncorrected wrong pixels


for b=1:Nban
  bands=max(1,b-nB):min(Nban,b+nB); %neighbouring bands for the spectral distance
  for r=1:Nrow
    for c=1:Ncol
      if M(r,c,b)==DOMV, %if the pixel is a drop-out we correct it
        C=N;             %used neighbours (C=N=1) of the 3x3 neighbourhood
        C(2,2)=0;        %we can not use the invalid pixel 
        if r==1,         %no upper row
          C(1,:)=0;  
        elseif r==Nrow,  %no bottom row
          C(3,:)=0; 
        end
        if c==1,         %no left column
          C(:,1)=0;
        elseif c==Ncol,  %no right column
          C(:,3)=0; 
        end
        W=zeros(3,3);    %matrix of convolution/average weights
        WS=0;            %sum of weights used to normalize W
        XC=0;            %corrected pixel value
        WS2=0;           %sum of weights used to normalize W
        XC2=0;           %average value of all neighbours (saturated or not)
        for ri=1:3
          for ci=1:3
            if C(ri,ci)~=0  %if the neighbour exist and is indicated in N we try to use it
              %Computation of the product neighbour*weight
              if M(r+ri-2,c+ci-2,b)==0  %if the neighbour band is valid we can use it
                if any(M(r,c,bands)==0)  %we need valid bands to compute the spectral distances
                  %We compute the Euclidean distance between pixels iteratively using only valid bands
                  D=0; k=0;
                  for bi=1:length(bands)
                    if M(r,c,bands(bi))==0 & M(r+ri-2,c+ci-2,bands(bi))==0
                      D=D+(X(r,c,bands(bi))-X(r+ri-2,c+ci-2,bands(bi)))^2;
                      k=k+1;
                    end
                  end
                  D=sqrt(D/k);  %Euclidean distance between corrected 
                  if D==0, D=eps; end %warning: division by cero
                  W(ri,ci)=1/D; %the weights are measure of similitude between pixels
                else %if all pixel bands are invalid but the neighbour band is valid
                  W(ri,ci)=1; %if no spectral distances can be used the weight is 1 for all neighbours
                end
                W(ri,ci)=W(ri,ci)*C(ri,ci);  %we can use values N~=1 to give different importance to each neighbouring position
                WS=WS+W(ri,ci);  %update of the sum of weights used to normalize W
                XC=XC+X(r+ri-2,c+ci-2,b)*W(ri,ci); %update of the weighted sum of the pixel value
              elseif M(r+ri-2,c+ci-2,b)==2 %invalid saturated neighbour (we use saturated only if there are no valid neighbours)
                W(ri,ci)=0;              %we do not mix valid and invalid neighbours
                WS2=WS2+C(ri,ci);          %update of the sum of neighbours used to normalize C
                XC2=XC2+X(r+ri-2,c+ci-2,b)*C(ri,ci); %update of the sum of the neighbour values
              else   %if the neighbour is invalid (M=1) it is not used in the correction
                W(ri,ci)=0;
              end
            end
          end
        end
        %Computation of the new corrected value for the pixel 
        if WS>0     %there are valid pixels to estimate the correct pixel value
          X(r,c,b)=XC/WS;    %we compute a weighted average with the valid neighbours
          M(r,c,b)=CDOMV;    %we denote the correction in the mask with a predefined mask value
        elseif WS2>0 %there are only saturated pixels to estimate the correct pixel value
          X(r,c,b)=XC2/WS2;  %we compute an average with the invalid neighbours (saturated) to fill the invalid drop-out
          M(r,c,b)=2;        %we denote the quasi-correction in the mask with the mask value of saturation (M=2)
        else         %there are valid pixels to estimate the correct pixel value
          X(r,c,b)=WRONG;    %we put a predefined wrong pixel value when no correction is possible
        end
      end
    end
  end
end


return

